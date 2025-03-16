package ru.anikson.cloudfilestorage.controller.minio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.service.minio.FileService;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService resourceService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo getResourceInfo(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return resourceService.getResourceInfo(username, path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        resourceService.deleteResource(username, path);
    }

    @GetMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public ByteArrayResource downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return resourceService.downloadResource(username, path);
    }

    @GetMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo moveResource(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return resourceService.moveResource(username, from, to);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> searchResources(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return resourceService.searchResources(username, query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfo> uploadResource(
            @RequestParam String path,
            @RequestParam("object") MultipartFile[] files,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        log.info("User {} upload file {}", username, files);
        return resourceService.uploadResources(username, path, files);
    }
}