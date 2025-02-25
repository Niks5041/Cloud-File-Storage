package ru.anikson.cloudfilestorage.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;

import java.io.InputStream;
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

    // Метод для получения папки пользователя
    private String getUserFolder(String userName) {
        return "user-" + userName + "-files"; // Формируем папку для пользователя, например "user-nik-files"
    }

    // Метод для получения информации о ресурсе (файле) по его пути
    public ResourceInfo getResourceInfo(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + "/" + path; // Полный путь до файла
        StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder() // Получаем информацию о файле
                .bucket(bucketName)
                .object(fullPath)
                .build());

        // Определяем, является ли объект папкой
        boolean isDirectory = fullPath.endsWith("/");

        // Если это папка, размер будет 0
        long size = isDirectory ? 0 : stat.size();

        // Возвращаем информацию о ресурсе: родительский путь, имя файла, размер, тип
        return new ResourceInfo(getParentPath(fullPath),
                getFileName(fullPath),
                size,
                isDirectory ? "DIRECTORY" : "FILE");
    }

    // Метод для удаления ресурса (файла) по пути
    public void deleteResource(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + "/" + path; // Формируем путь для удаления
        log.info("Удаление файла: {}", fullPath);
        minioClient.removeObject(RemoveObjectArgs.builder() // Удаляем файл
                .bucket(bucketName)
                .object(fullPath)
                .build());
    }

    // Метод для скачивания ресурса (файла) по пути
    public InputStream downloadResource(String userName, String path) throws Exception {
        String fullPath = getUserFolder(userName) + "/" + path; // Формируем путь для скачивания
        return minioClient.getObject(GetObjectArgs.builder() // Получаем объект для скачивания
                .bucket(bucketName)
                .object(fullPath)
                .build());
    }

    // Метод для перемещения ресурса (файла) из одного пути в другой
    public ResourceInfo moveResource(String userName, String from, String to) throws Exception {
        String userFolder = getUserFolder(userName); // Получаем папку пользователя
        String fromPath = userFolder + "/" + from; // Исходный путь
        String toPath = userFolder + "/" + to; // Целевой путь

        minioClient.copyObject(CopyObjectArgs.builder() // Копируем файл в новое место
                .bucket(bucketName)
                .source(CopySource.builder().bucket(bucketName).object(fromPath).build()) // Исходный объект
                .object(toPath) // Новый путь
                .build());

        deleteResource(userName, from); // Удаляем старый файл после копирования

        // Возвращаем информацию о новом ресурсе
        return new ResourceInfo(getParentPath(toPath), getFileName(toPath), null, "FILE");
    }

    // Метод для поиска ресурсов (файлов) по запросу
    public List<ResourceInfo> searchResources(String userName, String query) throws Exception {
        String userFolder = getUserFolder(userName); // Получаем папку пользователя
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder() // Получаем все объекты пользователя
                .bucket(bucketName)
                .recursive(true)
                .prefix(userFolder + "/")
                .build());

        // Преобразуем результаты в список объектов ResourceInfo
        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e); // Обработка ошибок при извлечении объекта
                    }
                })
                .filter(item -> item.objectName().contains(query)) // Фильтруем по запросу
                .map(item -> {
                    // Проверяем, является ли объект папкой
                    boolean isDirectory = item.objectName().endsWith("/");

                    // Для папки размер равен 0
                    long size = isDirectory ? 0 : item.size();
                    return new ResourceInfo(getParentPath(item.objectName()), getFileName(item.objectName()), size, isDirectory ? "DIRECTORY" : "FILE");
                })
                .collect(Collectors.toList());
    }

    // Метод для загрузки нескольких файлов для пользователя в указанную папку
    public List<ResourceInfo> uploadFiles(String userName, String path, MultipartFile[] files) throws Exception {
        String userFolder = getUserFolder(userName); // Получаем папку пользователя
        for (MultipartFile file : files) {
            String fullPath = userFolder + "/" + path + "/" + file.getOriginalFilename(); // Формируем путь для каждого файла
            minioClient.putObject(PutObjectArgs.builder() // Загружаем файл в MinIO
                    .bucket(bucketName)
                    .object(fullPath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        return List.of(); // Можно вернуть список информации о загруженных файлах (пока пустой)
    }

    // Вспомогательный метод для получения родительского пути
    private String getParentPath(String fullPath) {
        int lastSlashIndex = fullPath.lastIndexOf("/"); // Ищем последний слэш в пути
        return (lastSlashIndex == -1) ? "" : fullPath.substring(0, lastSlashIndex); // Возвращаем родительский путь
    }

    // Вспомогательный метод для получения имени файла
    private String getFileName(String fullPath) {
        return fullPath.substring(fullPath.lastIndexOf("/") + 1); // Получаем имя файла из пути
    }
}
