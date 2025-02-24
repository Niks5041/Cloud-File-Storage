package ru.anikson.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.service.MinioService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class MinioController {

    private final MinioService minioService;


    @GetMapping
    public List<String> listFiles(@RequestParam(value = "folder", defaultValue = "") String folder) throws Exception {
        log.info("Запрос списка файлов в папке: {}", folder);
        return minioService.listFiles(folder);  // Возвращаем список файлов напрямую
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public String uploadFile(@RequestParam("folder") String folder,
                             @RequestParam("file") MultipartFile file) throws Exception {
        log.info("Загрузка файла: {} в папку: {}", file.getOriginalFilename(), folder);
        minioService.uploadFile(folder, file);
        return "Файл успешно загружен";
    }

    @GetMapping("/download")
    public byte[] downloadFile(@RequestParam("file") String file) throws Exception {
        log.info("Запрос на скачивание файла: {}", file);
        return minioService.downloadFile(file).readAllBytes();  // Возвращаем файл как массив байтов
    }

    @PostMapping("/delete")
    @ResponseStatus(HttpStatus.OK)
    public String deleteFile(@RequestParam("file") String file) throws Exception {
        log.info("Запрос на удаление файла: {}", file);
        minioService.deleteFile(file);
        return "Файл успешно удален";
    }

    @PostMapping("/rename")
    @ResponseStatus(HttpStatus.OK)
    public String renameFile(@RequestParam("oldName") String oldName,
                             @RequestParam("newName") String newName) throws Exception {
        log.info("Запрос на переименование файла: {} в {}", oldName, newName);
        minioService.renameFile(oldName, newName);
        return "Файл успешно переименован";
    }
}
