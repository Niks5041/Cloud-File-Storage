package ru.anikson.cloudfilestorage.service;

import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
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

    private String getUserFolder(String userName) {
        return "user-" + userName + "-files/";
    }

    // --- Логика для папок ---

    public List<ResourceInfo> getDirectoryContents(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(false)
                        .build()
        );

        List<ResourceInfo> resources = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            boolean isDirectory = item.isDir();
            Long size = isDirectory ? null : item.size();
            resources.add(new ResourceInfo(
                    getParentPath(objectName),
                    getFileName(objectName),
                    size,
                    isDirectory ? "DIRECTORY" : "FILE"
            ));
        }

        // Если это корневая папка и она пуста, возвращаем пустой список вместо 404
        if (fullPath.equals(getUserFolder(userName)) && resources.isEmpty()) {
            log.info("Корневая папка {} пуста, возвращаем пустой список", fullPath);
            return resources;
        }

        // Для вложенных папок проверяем существование
        if (resources.isEmpty() && !resourceExists(fullPath)) {
            throw new NotFoundException("Папка не найдена: " + path);
        }

        return resources;
    }

    public ResourceInfo createDirectory(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }
        String parentPath = getParentPath(fullPath);
        log.info("Создание папки: fullPath={}, parentPath={}", fullPath, parentPath);
        String userFolder = getUserFolder(userName);
        if (!parentPath.equals(userFolder.trim()) && !parentPath.equals(userFolder) && !parentPath.isEmpty() && !resourceExists(parentPath + "/")) {
            throw new NotFoundException("Родительская папка не существует: " + parentPath);
        }
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .stream(InputStream.nullInputStream(), 0, -1)
                        .build()
        );
        log.info("Директория {} создана успешно", fullPath);
        return new ResourceInfo(getParentPath(fullPath), getFileName(fullPath), null, "DIRECTORY");
    }
    // --- Логика для файлов ---

    public ResourceInfo getResourceInfo(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build()
        );
        boolean isDirectory = fullPath.endsWith("/");
        Long size = isDirectory ? null : stat.size(); // Для папок size отсутствует
        String type = isDirectory ? "DIRECTORY" : "FILE";
        return new ResourceInfo(getParentPath(fullPath), getFileName(fullPath), size, type);
    }

    public void deleteResource(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        List<DeleteObject> deleteObjects = new ArrayList<>();
        for (Result<Item> object : objects) {
            deleteObjects.add(new DeleteObject(object.get().objectName()));
        }

        if (deleteObjects.isEmpty()) {
            throw new NotFoundException("Ресурс не найден: " + path);
        }

        minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build()
        );
        log.info("Ресурс удалён: {}", fullPath);
    }

    public InputStream downloadResource(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        if (fullPath.endsWith("/")) {
            throw new NotFoundException("Скачивание папок пока не поддерживается в формате zip: " + path);
            // TODO: Добавить поддержку скачивания папок как zip-архивов
        }
        log.info("Скачивание ресурса: {}", fullPath);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build()
        );
    }

    public List<ResourceInfo> uploadFiles(String userName, String path, MultipartFile[] files) throws Exception {
        String fullPathBase = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        if (!fullPathBase.endsWith("/")) {
            fullPathBase += "/";
        }
        if (!resourceExists(fullPathBase)) {
            throw new NotFoundException("Папка для загрузки не существует: " + path);
        }

        List<ResourceInfo> uploadedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String fullPath = fullPathBase + file.getOriginalFilename();
            createParentDirectories(fullPath); // Создаём вложенные папки, если есть в имени файла
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

    public ResourceInfo moveResource(String userName, String from, String to) throws Exception {
        String fromPath = getUserFolder(userName) + (from.startsWith("/") ? from.substring(1) : from);
        String toPath = getUserFolder(userName) + (to.startsWith("/") ? to.substring(1) : to);

        if (resourceExists(toPath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ресурс уже существует по пути: " + to);
        }
        if (!resourceExists(fromPath)) {
            throw new NotFoundException("Ресурс не найден: " + from);
        }

        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .source(CopySource.builder().bucket(bucketName).object(fromPath).build())
                        .object(toPath)
                        .build()
        );
        deleteResource(userName, from);
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder().bucket(bucketName).object(toPath).build()
        );
        boolean isDirectory = toPath.endsWith("/");
        Long size = isDirectory ? null : stat.size();
        return new ResourceInfo(getParentPath(toPath), getFileName(toPath), size, isDirectory ? "DIRECTORY" : "FILE");
    }

    public List<ResourceInfo> searchResources(String userName, String query) throws Exception {
        String userFolder = getUserFolder(userName);
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(userFolder)
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
                    Long size = isDirectory ? null : item.size();
                    return new ResourceInfo(getParentPath(item.objectName()), getFileName(item.objectName()), size, isDirectory ? "DIRECTORY" : "FILE");
                })
                .collect(Collectors.toList());
    }

    public void deleteDirectory(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + (path.startsWith("/") ? path.substring(1) : path);
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }

        if (!resourceExists(fullPath)) {
            throw new NotFoundException("Папка не найдена: " + path);
        }

        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        List<DeleteObject> deleteObjects = new ArrayList<>();
        for (Result<Item> object : objects) {
            deleteObjects.add(new DeleteObject(object.get().objectName()));
        }

        if (deleteObjects.isEmpty()) {
            throw new NotFoundException("Папка пуста или не существует: " + path);
        }

        minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build()
        );
        log.info("Папка удалена: {}", fullPath);
    }

    public ResourceInfo moveDirectory(String userName, String from, String to) throws Exception {
        String fromPath = getUserFolder(userName) + (from.startsWith("/") ? from.substring(1) : from);
        String toPath = getUserFolder(userName) + (to.startsWith("/") ? to.substring(1) : to);
        if (!fromPath.endsWith("/")) {
            fromPath += "/";
        }
        if (!toPath.endsWith("/")) {
            toPath += "/";
        }

        if (!resourceExists(fromPath)) {
            throw new NotFoundException("Папка не найдена: " + from);
        }
        if (resourceExists(toPath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Папка уже существует по пути: " + to);
        }

        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fromPath)
                        .recursive(true)
                        .build()
        );

        List<DeleteObject> deleteObjects = new ArrayList<>();
        for (Result<Item> object : objects) {
            Item item = object.get();
            String newPath = toPath + item.objectName().substring(fromPath.length());
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .source(CopySource.builder().bucket(bucketName).object(item.objectName()).build())
                            .object(newPath)
                            .build()
            );
            deleteObjects.add(new DeleteObject(item.objectName()));
        }

        if (deleteObjects.isEmpty()) {
            throw new NotFoundException("Папка пуста или не существует: " + from);
        }

        minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build()
        );

        log.info("Папка перемещена с {} на {}", fromPath, toPath);
        return new ResourceInfo(getParentPath(toPath), getFileName(toPath), null, "DIRECTORY");
    }

    // --- Вспомогательные методы ---

    private String getParentPath(String fullPath) {
        String trimmedPath = fullPath.endsWith("/") ? fullPath.substring(0, fullPath.length() - 1) : fullPath;
        int lastSlashIndex = trimmedPath.lastIndexOf("/");
        return (lastSlashIndex <= 0) ? "" : trimmedPath.substring(0, lastSlashIndex);
    }

    private String getFileName(String fullPath) {
        int lastSlashIndex = fullPath.lastIndexOf("/");
        return (lastSlashIndex == -1) ? fullPath : fullPath.substring(lastSlashIndex + 1);
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
            return false;
        }
    }

    private void createParentDirectories(String fullPath) throws Exception {
        String parentPath = getParentPath(fullPath);
        if (!parentPath.isEmpty() && !resourceExists(parentPath + "/")) {
            log.info("Создание родительской папки: {}", parentPath);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(parentPath + "/")
                            .stream(InputStream.nullInputStream(), 0, -1)
                            .build()
            );
        }
    }
}
