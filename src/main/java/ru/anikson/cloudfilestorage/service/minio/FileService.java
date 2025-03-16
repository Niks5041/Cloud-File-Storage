package ru.anikson.cloudfilestorage.service.minio;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.config.minio.MinioBucketConfiguration;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.exception.ValidationException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient; // Клиент для работы с MinIO
    private final MinioBucketConfiguration minioBucketConfiguration; // Конфигурация MinIO (имя бакета и т.д.)

    // Получение информации о ресурсе
    public ResourceInfo getResourceInfo(String username, String path) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        String fullPath = userPrefix + path; // Полный путь к ресурсу

        try {
            // Получаем метаданные объекта из MinIO
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                    .object(fullPath) // Указываем полный путь к объекту
                    .build());

            // Возвращаем информацию о ресурсе
            return ResourceInfo.builder()
                    .path(getParentPath(path)) // Путь к родительской папке
                    .name(getFileName(path)) // Имя файла или папки
                    .size(stat.size()) // Размер файла (если это файл)
                    .type("FILE") // Тип ресурса (FILE или DIRECTORY)
                    .build();
        } catch (Exception e) {
            throw new ValidationException("Resource not found: " + path); // Ошибка, если ресурс не найден
        }
    }

    // Удаление ресурса
    public void deleteResource(String username, String path) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        String fullPath = userPrefix + path; // Полный путь к ресурсу

        try {
            // Удаляем объект из MinIO
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                    .object(fullPath) // Указываем полный путь к объекту
                    .build());
        } catch (Exception e) {
            throw new ValidationException("Failed to delete resource: " + path); // Ошибка при удалении
        }
    }

    // Скачивание ресурса
    public ByteArrayResource downloadResource(String username, String path) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        String fullPath = userPrefix + path; // Полный путь к ресурсу

        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                .object(fullPath) // Указываем полный путь к объекту
                .build())) {
            // Читаем содержимое файла и возвращаем его как ByteArrayResource
            return new ByteArrayResource(stream.readAllBytes());
        } catch (Exception e) {
            throw new ValidationException("Failed to download resource: " + path); // Ошибка при скачивании
        }
    }

    // Перемещение или переименование ресурса
    public ResourceInfo moveResource(String username, String from, String to) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        String fullFromPath = userPrefix + from; // Полный путь к исходному объекту
        String fullToPath = userPrefix + to; // Полный путь к целевому объекту

        try {
            // Копируем объект в новое место
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                    .object(fullToPath) // Указываем целевой путь
                    .source(CopySource.builder()
                            .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                            .object(fullFromPath) // Указываем исходный путь
                            .build())
                    .build());

            // Удаляем исходный объект
            deleteResource(username, from);

            // Возвращаем информацию о перемещённом ресурсе
            return getResourceInfo(username, to);
        } catch (Exception e) {
            throw new ValidationException("Failed to move resource from " + from + " to " + to); // Ошибка при перемещении
        }
    }

    // Поиск ресурсов
    public List<ResourceInfo> searchResources(String username, String query) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        List<ResourceInfo> resources = new ArrayList<>();

        try {
            // Получаем список объектов в бакете
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                    .prefix(userPrefix) // Указываем префикс пользователя
                    .recursive(true) // Рекурсивный поиск
                    .build());

            // Фильтруем объекты по запросу
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.objectName().contains(query)) {
                    resources.add(ResourceInfo.builder()
                            .path(getParentPath(item.objectName().replace(userPrefix, ""))) // Путь к родительской папке
                            .name(getFileName(item.objectName())) // Имя файла или папки
                            .size(item.isDir() ? null : item.size()) // Размер файла (если это файл)
                            .type(item.isDir() ? "DIRECTORY" : "FILE") // Тип ресурса
                            .build());
                }
            }
        } catch (Exception e) {
            throw new ValidationException("Failed to search resources: " + query); // Ошибка при поиске
        }

        return resources;
    }

    // Загрузка ресурсов
    public List<ResourceInfo> uploadResources(String username, String path, MultipartFile[] files) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        List<ResourceInfo> uploadedResources = new ArrayList<>();

        for (MultipartFile file : files) {
            try (InputStream stream = file.getInputStream()) {
                // Формируем полный путь к файлу
                String filePath = userPrefix + path + file.getOriginalFilename();

                // Загружаем файл в MinIO
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                        .object(filePath) // Указываем полный путь к файлу
                        .stream(stream, file.getSize(), -1) // Передаем содержимое файла
                        .build());

                // Добавляем информацию о загруженном файле в результат
                uploadedResources.add(ResourceInfo.builder()
                        .path(path) // Путь к папке
                        .name(file.getOriginalFilename()) // Имя файла
                        .size(file.getSize()) // Размер файла
                        .type("FILE") // Тип ресурса
                        .build());
            } catch (Exception e) {
                throw new ValidationException("Failed to upload file: " + file.getOriginalFilename()); // Ошибка при загрузке
            }
        }
        return uploadedResources;
    }

    // Формирование префикса пользователя
    private String getUserPrefix(String username) {
        return "user-" + username + "-files/"; // Префикс в формате user-${username}-files/
    }

    // Получение пути к родительской папке
    private String getParentPath(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex > 0 ? path.substring(0, lastSlashIndex + 1) : "/";
    }

    // Получение имени файла или папки
    private String getFileName(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex > 0 ? path.substring(lastSlashIndex + 1) : path;
    }
}