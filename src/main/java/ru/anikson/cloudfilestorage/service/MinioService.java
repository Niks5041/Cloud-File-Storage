package ru.anikson.cloudfilestorage.service;

import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.exception.NotFoundException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void initializeBucket() {
        try {
            // Проверяем, существует ли бакет
            boolean isExist = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!isExist) {
                // Если бакет не существует, создаем его
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Бакет {} успешно создан", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации бакета: {}", e.getMessage(), e);
        }
    }

//    @PostConstruct
//    public void initializeUserFolders() {
//        try {
//            String userName = SecurityContextHolder.getContext().getAuthentication().getName(); // Получаем имя пользователя
//            String userFolder = "user-" + userName + "-files/";
//
//            // Проверяем, существует ли папка для пользователя
//            if (!resourceExists(userFolder)) {
//                minioClient.putObject(
//                        PutObjectArgs.builder()
//                                .bucket(bucketName)
//                                .object(userFolder) // создаём папку для пользователя
//                                .stream(InputStream.nullInputStream(), 0, -1)
//                                .build()
//                );
//                log.info("Создана папка для пользователя {}: {}", userName, userFolder);
//            }
//        } catch (Exception e) {
//            log.error("Ошибка инициализации папок для пользователей: {}", e.getMessage(), e);
//        }
//    }

    private String getUserFolder(String userName) {
        return "user-" + userName + "-files"; // Папка пользователя
    }

    // Получение содержимого директории
    public List<ResourceInfo> getDirectoryContents(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path : "/" + path);
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1); // Убираем последний слеш
        }

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath + "/") // Содержимое папки
                        .recursive(false) // Только текущий уровень
                        .build()
        );

        List<ResourceInfo> resources = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            boolean isDirectory = item.isDir();
            long size = isDirectory ? 0 : item.size();
            resources.add(new ResourceInfo(
                    getParentPath(objectName),
                    getFileName(objectName),
                    size,
                    isDirectory ? "DIRECTORY" : "FILE"
            ));
        }
        return resources;
    }

    // Создание директории
    public ResourceInfo createDirectory(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path : "/" + path);
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }

        createParentDirectories(fullPath); // <-- Добавили вызов

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .stream(InputStream.nullInputStream(), 0, -1)
                        .build()
        );

        log.info("Директория {} создана успешно", fullPath);
        return new ResourceInfo(getParentPath(fullPath), getFileName(fullPath), 0L, "DIRECTORY");
    }


    // Получение информации о ресурсе
    public ResourceInfo getResourceInfo(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path : "/" + path);
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build()
        );
        boolean isDirectory = fullPath.endsWith("/");
        long size = isDirectory ? null : stat.size();
        return new ResourceInfo(getParentPath(fullPath), getFileName(fullPath), size, isDirectory ? "DIRECTORY" : "FILE");
    }

    // Удаление ресурса
    public void deleteResource(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path : "/" + path);
        fullPath = fullPath.replaceFirst("^" + getUserFolder(userName), ""); // Убираем дублирование

        log.info("Удаление ресурса: {}", fullPath);

        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath) // Получаем все файлы с таким префиксом
                        .recursive(true)  // Рекурсивно, чтобы захватить все вложенные файлы
                        .build()
        );

        List<DeleteObject> deleteObjects = new ArrayList<>();
        for (Result<Item> object : objects) {
            deleteObjects.add(new DeleteObject(object.get().objectName()));
        }

        if (!deleteObjects.isEmpty()) {
            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build()
            );
            log.info("Удалены файлы в папке: {}", fullPath);
        } else {
            log.warn("Папка пуста или не найдена: {}", fullPath);
            throw new NotFoundException("Ресурс не найден по пути: " + fullPath);  // Добавление пути в сообщение
        }
    }


    // Скачивание ресурса
    public InputStream downloadResource(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path : "/" + path);
        log.info("Скачивание ресурса: {}", fullPath);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build()
        );
    }

    // Перемещение ресурса
    public ResourceInfo moveResource(String userName, String from, String to) throws Exception {
        String fromPath = getUserFolder(userName) + (from.startsWith("/") ? from : "/" + from);
        String toPath = getUserFolder(userName) + (to.startsWith("/") ? to : "/" + to);

        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .source(CopySource.builder().bucket(bucketName).object(fromPath).build())
                        .object(toPath)
                        .build()
        );
        deleteResource(userName, from);
        return new ResourceInfo(getParentPath(toPath), getFileName(toPath), null, "FILE");
    }

    // Поиск ресурсов
    public List<ResourceInfo> searchResources(String userName, String query) throws Exception {
        String userFolder = getUserFolder(userName);
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(userFolder + "/")
                        .recursive(true)
                        .build()
        );

        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(item -> item.objectName().contains(query))
                .map(item -> {
                    boolean isDirectory = item.isDir();
                    long size = isDirectory ? 0 : item.size();
                    return new ResourceInfo(getParentPath(item.objectName()), getFileName(item.objectName()), size, isDirectory ? "DIRECTORY" : "FILE");
                })
                .collect(Collectors.toList());
    }

    // Загрузка файлов
    public List<ResourceInfo> uploadFiles(String userName, String path, MultipartFile[] files) throws Exception {
        String userFolder = getUserFolder(userName);
        String fullPathBase = userFolder + (path.startsWith("/") ? path : "/" + path);
        if (!fullPathBase.endsWith("/")) {
            fullPathBase += "/";
        }

        List<ResourceInfo> uploadedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String fullPath = fullPathBase + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            uploadedFiles.add(new ResourceInfo(getParentPath(fullPath), getFileName(fullPath), file.getSize(), "FILE"));
        }
        return uploadedFiles;
    }

    private String getParentPath(String fullPath) {
        int lastSlashIndex = fullPath.lastIndexOf("/");
        return (lastSlashIndex == -1) ? "" : fullPath.substring(0, lastSlashIndex);
    }

    private String getFileName(String fullPath) {
        return fullPath.substring(fullPath.lastIndexOf("/") + 1);
    }

    private boolean resourceExists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("Ошибка при проверке ресурса: {}", path, e); // Логирование ошибок
            return false;
        }
    }


    private void createParentDirectories(String fullPath) throws Exception {
        String parentPath = getParentPath(fullPath);
        if (!parentPath.equals("/") && !resourceExists(parentPath)) {
            log.info("Создаем родительскую папку: {}", parentPath);
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(parentPath + "/") // В MinIO директории обозначаются слэшем
                                .stream(InputStream.nullInputStream(), 0, -1)
                                .build()
                );
                log.info("Родительская папка {} успешно создана", parentPath);
            } catch (Exception e) {
                log.error("Ошибка при создании родительской папки: {}", parentPath, e);
                throw e; // Пробрасываем ошибку дальше, если что-то пошло не так
            }
        }
    }
}