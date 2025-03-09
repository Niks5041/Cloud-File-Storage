package ru.anikson.cloudfilestorage.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBucket() {
        try {
            boolean isExist = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Бакет {} успешно создан", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации бакета: {}", e.getMessage(), e);
        }
    }

    public ResourceInfo getResourceInfo(String username, String path) {
        try {
            String userRootPath = getUserRootPath(username);
            String fullPath = userRootPath + (path.equals("/") ? "" : path);
            StatObjectResponse stat;

            try {
                // Проверяем как файл
                stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
            } catch (Exception e) {
                // Проверяем как директорию
                String dirPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";
                stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(dirPath)
                                .build()
                );
            }

            ResourceInfo resourceInfo = new ResourceInfo();
            resourceInfo.setPath(getParentPath(path));
            resourceInfo.setName(getFileName(path));
            resourceInfo.setSize(stat.size());
            resourceInfo.setType(stat.size() >= 0 ? "FILE" : "DIRECTORY");
            return resourceInfo;
        } catch (Exception e) {
            log.error("Ошибка при получении информации о ресурсе {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error getting resource info", e);
        }
    }

    public void deleteResource(String username, String path) {
        try {
            String fullPath = getUserRootPath(username) + (path.equals("/") ? "" : path);

            // Проверяем, является ли это папкой (если есть вложенные файлы)
            if (isDirectory(fullPath)) {
                // Получаем все объекты в директории и удаляем их
                List<String> objectNames = getObjectsInDirectory(fullPath);
                for (String objectName : objectNames) {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .build()
                    );
                    log.info("Ресурс {} успешно удален для пользователя {}", objectName, username);
                }
            } else {
                // Если это файл, просто удаляем его
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
                log.info("Ресурс {} успешно удален для пользователя {}", fullPath, username);
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении ресурса {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error deleting resource", e);
        }
    }

    private boolean isDirectory(String path) {
        // Для определения, является ли объект папкой, можно попытаться получить объекты с префиксом этого пути.
        try {
            List<String> objects = getObjectsInDirectory(path);
            return !objects.isEmpty(); // Если в папке есть объекты, значит это папка
        } catch (Exception e) {
            return false; // В случае ошибки считаем, что это файл
        }
    }

    private List<String> getObjectsInDirectory(String directoryPath) {
        try {
            // Создаем список для хранения имен объектов
            List<String> objectNames = new ArrayList<>();

            // Получаем список объектов по префиксу
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(directoryPath)
                            .recursive(true)
                            .build()
            );

            // Проходим по результатам и добавляем имена объектов в список
            for (Result<Item> result : results) {
                Item item = result.get();
                objectNames.add(item.objectName());
            }

            return objectNames;
        } catch (Exception e) {
            log.error("Ошибка при получении объектов из директории: {}", directoryPath, e);
            return Collections.emptyList(); // В случае ошибки возвращаем пустой список
        }
    }

    public InputStream downloadResource(String username, String path) {
        try {
            String userRootPath = getUserRootPath(username);
            String fullPath = userRootPath + (path.equals("/") ? "" : path);

            if (path.endsWith("/")) {
                return createZipFromDirectory(username, fullPath).getInputStream();
            }

            try {
                // Проверяем как файл
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
            } catch (Exception e) {
                // Проверяем как директорию
                String dirPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";
                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(dirPath)
                                .build()
                );
                if (stat.size() < 0) { // Это директория
                    return createZipFromDirectory(username, dirPath).getInputStream();
                }
                throw e; // Если это не директория и не файл, бросаем исходную ошибку
            }
        } catch (Exception e) {
            log.error("Ошибка при скачивании ресурса {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error downloading resource", e);
        }
    }

    public ResourceInfo moveResource(String username, String from, String to) {
        try {
            String userRootPath = getUserRootPath(username);
            String fullFromPath = userRootPath + (from.equals("/") ? "" : from);
            String fullToPath = userRootPath + (to.equals("/") ? "" : to);
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullToPath)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(fullFromPath)
                                    .build())
                            .build()
            );
            deleteResource(username, from);
            log.info("Ресурс успешно перемещен с {} на {} для пользователя {}", from, to, username);
            return getResourceInfo(username, to);
        } catch (Exception e) {
            log.error("Ошибка при перемещении ресурса с {} на {} для пользователя {}: {}", from, to, username, e.getMessage());
            throw new RuntimeException("Error moving resource", e);
        }
    }

    public List<ResourceInfo> searchResources(String username, String query) {
        List<ResourceInfo> results = new ArrayList<>();
        try {
            String userPrefix = getUserRootPath(username);
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(userPrefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : objects) {
                Item item = result.get();
                String relativePath = item.objectName().substring(userPrefix.length());
                if (relativePath.contains(query)) {
                    results.add(convertItemToDto(relativePath, item));
                }
            }
            log.info("Найдено {} ресурсов по запросу {} для пользователя {}", results.size(), query, username);
            return results;
        } catch (Exception e) {
            log.error("Ошибка при поиске ресурсов по запросу {} для пользователя {}: {}", query, username, e.getMessage());
            throw new RuntimeException("Error searching resources", e);
        }
    }

    public List<ResourceInfo> uploadFiles(String username, String path, MultipartFile[] files) {
        List<ResourceInfo> uploaded = new ArrayList<>();
        try {
            String userPath = getUserRootPath(username) + (path.equals("/") ? "" : path);
            if (!userPath.endsWith("/")) {
                userPath += "/";
            }
            for (MultipartFile file : files) {
                String objectName = userPath + file.getOriginalFilename();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .build()
                );
                uploaded.add(getResourceInfo(username, (path.equals("/") ? "" : path) + "/" + file.getOriginalFilename()));
            }
            log.info("Успешно загружено {} файлов в {} для пользователя {}", uploaded.size(), path, username);
            return uploaded;
        } catch (Exception e) {
            log.error("Ошибка при загрузке ресурсов в {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error uploading resource", e);
        }
    }

    public List<ResourceInfo> getDirectoryContents(String username, String path) {
        List<ResourceInfo> contents = new ArrayList<>();
        try {
            String userRootPath = getUserRootPath(username);
            String fullPath = userRootPath + (path.equals("/") ? "" : path);
            if (!fullPath.endsWith("/")) {
                fullPath += "/"; // Всегда добавляем / для директорий
            }
            log.debug("Полный путь для getDirectoryContents: {}", fullPath);

            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(false)
                            .build()
            );

            for (Result<Item> result : objects) {
                Item item = result.get();
                String relativePath = item.objectName().substring(userRootPath.length());
                contents.add(convertItemToDto(relativePath, item));
            }
            log.info("Получено содержимое директории {}: {} элементов для пользователя {}", path, contents.size(), username);
            return contents;
        } catch (Exception e) {
            log.error("Ошибка при получении содержимого директории {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error getting directory content", e);
        }
    }

    public ResourceInfo createDirectory(String username, String path) {
        try {
            String userRootPath = getUserRootPath(username);
            String fullPath = userRootPath + (path.equals("/") ? "" : path);
            if (!fullPath.endsWith("/")) {
                fullPath += "/";
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .stream(new java.io.ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
            log.info("Директория {} успешно создана для пользователя {}", path, username);
            ResourceInfo resourceInfo = new ResourceInfo();
            resourceInfo.setPath(getParentPath(path));
            resourceInfo.setName(getFileName(path));
            resourceInfo.setType("DIRECTORY");
            return resourceInfo;
        } catch (Exception e) {
            log.error("Ошибка при создании директории {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error creating directory", e);
        }
    }

    public void deleteDirectory(String username, String path) {
        try {
            String userRootPath = getUserRootPath(username);
            String fullPath = userRootPath + (path.equals("/") ? "" : path);
            if (!fullPath.endsWith("/")) {
                fullPath += "/";
            }
            log.debug("Полный путь для deleteDirectory: {}", fullPath);

            // Удаляем все объекты с префиксом
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true)
                            .build()
            );
            boolean hasObjects = false;
            for (Result<Item> result : objects) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build()
                );
                hasObjects = true;
            }

            // Удаляем саму директорию, если она существует как объект
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
                hasObjects = true;
            } catch (Exception ignored) {
                // Если объекта нет, это нормально, продолжаем
            }

            if (hasObjects) {
                log.info("Директория {} успешно удалена для пользователя {}", path, username);
            } else {
                log.warn("Директория {} не найдена или пуста для пользователя {}", path, username);
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении директории {} для пользователя {}: {}", path, username, e.getMessage());
            throw new RuntimeException("Error deleting directory", e);
        }
    }

    public ResourceInfo moveDirectory(String username, String from, String to) {
        try {
            List<ResourceInfo> contents = getDirectoryContents(username, from);
            for (ResourceInfo item : contents) {
                String fromPath = (from.equals("/") ? "" : from) + (from.endsWith("/") ? "" : "/") + item.getName();
                String toPath = (to.equals("/") ? "" : to) + (to.endsWith("/") ? "" : "/") + item.getName();
                moveResource(username, fromPath, toPath);
            }
            deleteDirectory(username, from);
            log.info("Директория успешно перемещена с {} на {} для пользователя {}", from, to, username);
            return getResourceInfo(username, to);
        } catch (Exception e) {
            log.error("Ошибка при перемещении директории с {} на {} для пользователя {}: {}", from, to, username, e.getMessage());
            throw new RuntimeException("Error moving directory", e);
        }
    }

    private ByteArrayResource createZipFromDirectory(String username, String path) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String fullPath = path; // Уже полный путь передан из downloadResource
            if (!fullPath.endsWith("/")) {
                fullPath += "/";
            }
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : objects) {
                Item item = result.get();
                if (!item.isDir()) {
                    GetObjectResponse file = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(item.objectName())
                                    .build()
                    );
                    zos.putNextEntry(new ZipEntry(item.objectName().substring(fullPath.length())));
                    file.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
        log.info("Создана ZIP-архивация директории {} для пользователя {}", path, username);
        return new ByteArrayResource(baos.toByteArray());
    }

    private String getUserRootPath(String username) {
        return "user-" + username + "-files/";
    }

    private String getParentPath(String path) {
        if (path.equals("/")) {
            return "";
        }
        String normalized = path.replaceAll("^/+|/+$", "");
        int lastSlash = normalized.lastIndexOf("/");
        return lastSlash >= 0 ? normalized.substring(0, lastSlash) : "";
    }

    private String getFileName(String path) {
        if (path.equals("/")) {
            return "";
        }
        String normalized = path.replaceAll("^/+|/+$", "");
        return normalized.isEmpty() ? "" : normalized.substring(normalized.lastIndexOf("/") + 1);
    }

    private ResourceInfo convertItemToDto(String relativePath, Item item) {
        ResourceInfo resourceInfo = new ResourceInfo();
        resourceInfo.setPath(getParentPath(relativePath));
        resourceInfo.setName(getFileName(relativePath));
        resourceInfo.setSize(item.size());
        resourceInfo.setType(item.isDir() ? "DIRECTORY" : "FILE");
        return resourceInfo;
    }
}

