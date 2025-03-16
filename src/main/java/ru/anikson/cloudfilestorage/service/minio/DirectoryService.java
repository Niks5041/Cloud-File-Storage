package ru.anikson.cloudfilestorage.service.minio;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.anikson.cloudfilestorage.config.minio.MinioBucketConfiguration;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.exception.ValidationException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final MinioClient minioClient; // Клиент для работы с MinIO
    private final MinioBucketConfiguration minioBucketConfiguration; // Конфигурация MinIO (имя бакета и т.д.)

    // Получение содержимого папки
    public List<ResourceInfo> getDirectoryContent(String username, String path) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        String fullPath = userPrefix + path; // Полный путь к папке

        try {
            List<ResourceInfo> resources = new ArrayList<>();

            // Получаем список объектов в папке
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                    .prefix(fullPath) // Указываем полный путь к папке
                    .recursive(false) // Не рекурсивно
                    .build());

            // Формируем информацию о каждом объекте
            for (Result<Item> result : results) {
                Item item = result.get();
                resources.add(ResourceInfo.builder()
                        .path(path) // Путь к папке
                        .name(item.objectName().replace(fullPath, "")) // Имя файла или папки
                        .size(item.isDir() ? null : item.size()) // Размер файла (если это файл)
                        .type(item.isDir() ? "DIRECTORY" : "FILE") // Тип ресурса
                        .build());
            }
            return resources;
        } catch (Exception e) {
            throw new ValidationException("Failed to get directory content: " + path); // Ошибка при получении содержимого
        }
    }

    // Создание папки
    public ResourceInfo createDirectory(String username, String path) {
        String userPrefix = getUserPrefix(username); // Формируем префикс пользователя
        String fullPath = userPrefix + path; // Полный путь к папке

        try {
            // Убедимся, что путь заканчивается на "/"
            if (!fullPath.endsWith("/")) {
                fullPath += "/";
            }

            // Создаем папку в MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioBucketConfiguration.getBucketName()) // Указываем бакет
                    .object(fullPath) // Указываем полный путь к папке
                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1) // Пустой поток для создания папки
                    .build());

            // Возвращаем информацию о созданной папке
            return ResourceInfo.builder()
                    .path(getParentPath(path)) // Путь к родительской папке
                    .name(getFileName(path)) // Имя папки
                    .type("DIRECTORY") // Тип ресурса
                    .build();
        } catch (Exception e) {
            throw new ValidationException("Failed to create directory: " + path); // Ошибка при создании папки
        }
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