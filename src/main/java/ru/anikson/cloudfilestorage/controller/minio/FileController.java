package ru.anikson.cloudfilestorage.controller.minio;

import jakarta.validation.ValidationException;
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
import ru.anikson.cloudfilestorage.service.MinioService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final MinioService minioService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo getResourceInfo(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String path) throws Exception {
        log.info("GET /api/resource с путём: {}", path);
        validateUser(userDetails);
        if (path.isBlank()) {
            throw new ValidationException("Путь не может быть пустым");
        }
        return minioService.getResourceInfo(userDetails.getUsername(), path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String path) throws Exception {
        log.info("DELETE /api/resource с путём: {}", path);
        validateUser(userDetails);
        if (path.isBlank()) {
            throw new ValidationException("Путь не может быть пустым");
        }
        try {
            minioService.deleteResource(userDetails.getUsername(), path);
        } catch (Exception e) {
            log.error("Неизвестная ошибка при удалении ресурса: {}", path, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Неизвестная ошибка при удалении ресурса", e);
        }
    }

    @GetMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public byte[] downloadResource(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String path) throws Exception {
        log.info("GET /api/resource/download с путём: {}", path);
        validateUser(userDetails);
        if (path.isBlank()) {
            throw new ValidationException("Путь не может быть пустым");
        }
        InputStream inputStream = minioService.downloadResource(userDetails.getUsername(), path);
        return inputStream.readAllBytes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfo> uploadResource(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam("path") String path,
                                             @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        log.info("POST /api/resource с путём: {}", path);
        if (files == null || files.length == 0) {
            log.warn("Файлы для загрузки не предоставлены");
            return new ArrayList<>();
        }
        log.debug("Получен запрос с path: {}, files: {}", path, files != null ? files.length : "null");
        return minioService.uploadFiles(userDetails.getUsername(), path, files);
    }

    @PostMapping("/move") // Изменил с GET на POST согласно логике перемещения
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo moveResource(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestParam String from,
                                     @RequestParam String to) throws Exception {
        log.info("POST /api/resource/move с пути: {} на путь: {}", from, to);
        validateUser(userDetails);
        if (from.isBlank() || to.isBlank()) {
            throw new ValidationException("Пути 'from' или 'to' не могут быть пустыми");
        }
        return minioService.moveResource(userDetails.getUsername(), from, to);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> searchResource(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam String query) throws Exception {
        log.info("GET /api/resource/search с запросом: {}", query);
        validateUser(userDetails);
        if (query.isBlank()) {
            throw new ValidationException("Поисковый запрос не может быть пустым");
        }
        return minioService.searchResources(userDetails.getUsername(), query);
    }

    private void validateUser(UserDetails userDetails) {
        if (userDetails == null || !userDetails.isEnabled()) {
            log.error("Пользователь не найден или не активен");
            throw new NotFoundException("Пользователь не найден или не активен");
        }
    }
}

