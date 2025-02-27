package ru.anikson.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.exception.NotFoundException;
import ru.anikson.cloudfilestorage.service.MinioService;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class MinioController {

    private final MinioService minioService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo getResourceInfo(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String path) throws Exception {
        log.info("GET /files/resource");
        validateUser(userDetails);
        return minioService.getResourceInfo(userDetails.getUsername(), path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String path) throws Exception {
        log.info("DELETE /files/resource");
        validateUser(userDetails);
        minioService.deleteResource(userDetails.getUsername(), path);
    }

    @GetMapping("/download")
    public byte[] downloadResource(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam String path) throws Exception {
        log.info("GET /files/resource/download");
        validateUser(userDetails);
        InputStream inputStream = minioService.downloadResource(userDetails.getUsername(), path);
        return inputStream.readAllBytes();
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfo> uploadResource(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam String path,
                                             @RequestParam MultipartFile[] files) throws Exception {
        log.info("POST /files/resource");
        validateUser(userDetails);
        return minioService.uploadFiles(userDetails.getUsername(), path, files);
    }

    @GetMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo moveResource(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestParam String from,
                                     @RequestParam String to) throws Exception {
        log.info("GET /files/resource/move");
        validateUser(userDetails);
        return minioService.moveResource(userDetails.getUsername(), from, to);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> searchResource(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam String query) throws Exception {
        log.info("GET /resource/search");
        validateUser(userDetails);
        return minioService.searchResources(userDetails.getUsername(), query);
    }

    private void validateUser(UserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            log.error("Пользователь не найден");
            throw new NotFoundException("Пользователь не найден");
        }
    }
}
