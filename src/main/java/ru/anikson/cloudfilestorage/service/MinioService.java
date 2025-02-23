package ru.anikson.cloudfilestorage.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient; // Клиент для работы с MinIO

    @Value("${minio.bucket-name}")  // Имя бакета, куда загружаются файлы
    private String bucketName;

    public void uploadFile(String folder, MultipartFile file) throws Exception {
        // Формируем имя файла в MinIO
        String objectName = folder.isEmpty() ? file.getOriginalFilename() : folder + "/" + file.getOriginalFilename();

        // Загружаем файл в MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)  // Бакет, куда загружаем
                        .object(objectName)  // Имя объекта в MinIO
                        .stream(file.getInputStream(), file.getSize(), -1)  // Поток файла
                        .contentType(file.getContentType())  // MIME-тип файла
                        .build()
        );
    }

    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)  // Бакет, откуда скачиваем
                        .object(objectName)  // Имя файла
                        .build()
        );
    }

    public List<String> listFiles(String folder) throws Exception {
        List<String> fileNames = new ArrayList<>();

        // Получаем список объектов из Minio, которые находятся в указанной папке (folder) в бакете
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)  // Указываем бакет, в котором нужно искать файлы
                        .prefix(folder)  // Указываем префикс (папку), чтобы ограничить поиск только внутри этой папки
                        .build()
        );

        for (Result<Item> result : objects) {
            try {
                // Получаем имя объекта (файла) и добавляем в список
                fileNames.add(result.get().objectName());
            } catch (Exception e) {
            }
        }

        return fileNames;
    }
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)  // Бакет, где удаляем файл
                        .object(objectName)  // Имя файла
                        .build()
        );
    }

    public void renameFile(String oldName, String newName) throws Exception {
        // Сначала копируем файл в MinIO под новым именем
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .source(CopySource.builder().bucket(bucketName).object(oldName).build()) // Источник (старое имя)
                        .bucket(bucketName)  // Бакет, куда копируем
                        .object(newName)  // Новое имя файла
                        .build()
        );
        // Удаляем старый файл
        deleteFile(oldName);
    }
}
