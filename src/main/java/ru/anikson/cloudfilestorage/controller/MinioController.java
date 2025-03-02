package ru.anikson.cloudfilestorage.controller;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.exception.NotFoundException;
import ru.anikson.cloudfilestorage.exception.ValidationException;
import ru.anikson.cloudfilestorage.service.MinioService;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api") // Соответствует API_CONTEXT
@RequiredArgsConstructor
@Slf4j
public class MinioController {

    private final MinioService minioService;

    // Работа с папками (/directory)
    @GetMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> getDirectoryContents(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestParam(required = false, defaultValue = "/") String path) throws Exception {
        log.info("GET /api/directory with path: {}", path);

        if (path.isBlank()) {
            throw new ValidationException("Path cannot be empty");
        }

        validateUser(userDetails);
        return minioService.getDirectoryContents(userDetails.getUsername(), path);
    }


    @PostMapping("/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfo createDirectory(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String path) throws Exception {
        log.info("POST /api/directory with path: {}", path);
        validateUser(userDetails);
        return minioService.createDirectory(userDetails.getUsername(), path);
    }

    // Работа с файлами (/resource)
    @GetMapping("/resource")
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo getResourceInfo(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String path) throws Exception {
        log.info("GET /api/resource with path: {}", path);
        validateUser(userDetails);
        return minioService.getResourceInfo(userDetails.getUsername(), path);
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String path) throws Exception {
        log.info("DELETE /api/resource с путём: {}", path);

        // Валидация пользователя
        validateUser(userDetails);

        // Проверка пути
        if (path == null || path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Путь не может быть пустым");
        }

        // Вызов сервиса для удаления ресурса
        try {
            minioService.deleteResource(userDetails.getUsername(), path);
        } catch (MinioException e) {
            log.error("Ошибка при удалении ресурса: {}", path, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ресурс не найден");
        } catch (Exception e) {
            log.error("Неизвестная ошибка при удалении ресурса: {}", path, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Неизвестная ошибка при удалении ресурса", e);
        }
    }

    @GetMapping("/resource/download")
    @ResponseStatus(HttpStatus.OK)
    public byte[] downloadResource(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String path) throws Exception {
        log.info("GET /api/resource/download with path: {}", path);
        validateUser(userDetails);
        InputStream inputStream = minioService.downloadResource(userDetails.getUsername(), path);
        return inputStream.readAllBytes();
    }

    @PostMapping("/resource")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfo> uploadResource(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam String path,
                                             @RequestParam MultipartFile[] files) throws Exception {
        log.info("POST /api/resource with path: {}", path);
        validateUser(userDetails);
        return minioService.uploadFiles(userDetails.getUsername(), path, files);
    }

    @PostMapping("/resource/move") // Исправлено с GET на POST
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo moveResource(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestParam String from,
                                     @RequestParam String to) throws Exception {
        log.info("POST /api/resource/move from: {} to: {}", from, to);
        validateUser(userDetails);
        return minioService.moveResource(userDetails.getUsername(), from, to);
    }

    @GetMapping("/resource/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> searchResource(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam String query) throws Exception {
        log.info("GET /api/resource/search with query: {}", query);
        validateUser(userDetails);
        return minioService.searchResources(userDetails.getUsername(), query);
    }

    private void validateUser(UserDetails userDetails) {
        if (userDetails == null || !userDetails.isEnabled()) {
            log.error("Пользователь не найден или не активен");
            throw new NotFoundException("Пользователь не найден или не активен");
        }
    }
}