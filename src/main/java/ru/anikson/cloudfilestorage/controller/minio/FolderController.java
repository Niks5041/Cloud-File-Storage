package ru.anikson.cloudfilestorage.controller.minio;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.exception.NotFoundException;
import ru.anikson.cloudfilestorage.service.MinioService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FolderController {

    private final MinioService minioService;

    @PutMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> renameDir(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String from,
                                        @RequestParam String to,
                                        @RequestParam String path) throws Exception {
        log.info("PUT /api/directory с путём: {}", path);
        validateUser(userDetails);
        if (path.isBlank()) {
            throw new ValidationException("Путь не может быть пустым");
        }
        return minioService.renameDir(userDetails.getUsername(), from, to, path);
    }

    @GetMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> getDirectoryContents(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestParam(required = false, defaultValue = "/") String path) throws Exception {
        log.info("GET /api/directory с путём: {}", path);
        validateUser(userDetails);
        if (path.isBlank()) {
            throw new ValidationException("Путь не может быть пустым");
        }
        return minioService.getDirectoryContents(userDetails.getUsername(), path);
    }

    @PostMapping("/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfo createDirectory(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String path) throws Exception {
        log.info("POST /api/directory с путём: {}", path);
        validateUser(userDetails);
        if (path.isBlank()) {
            throw new ValidationException("Путь не может быть пустым");
        }
        return minioService.createDirectory(userDetails.getUsername(), path);
    }

    @PostMapping("/directory/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo moveDirectory(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestParam String from,
                                      @RequestParam String to) throws Exception {
        log.info("POST /api/directory/move с пути: {} на путь: {}", from, to);
        validateUser(userDetails);
        if (from.isBlank() || to.isBlank()) {
            throw new ValidationException("Пути 'from' или 'to' не могут быть пустыми");
        }
        return minioService.moveDirectory(userDetails.getUsername(), from, to);
    }

    private void validateUser(UserDetails userDetails) {
        if (userDetails == null || !userDetails.isEnabled()) {
            log.error("Пользователь не найден или не активен");
            throw new NotFoundException("Пользователь не найден или не активен");
        }
    }
}

