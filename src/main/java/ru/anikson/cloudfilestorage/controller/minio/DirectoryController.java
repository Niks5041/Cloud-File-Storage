package ru.anikson.cloudfilestorage.controller.minio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.anikson.cloudfilestorage.entity.ResourceInfo;
import ru.anikson.cloudfilestorage.service.minio.DirectoryService;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    public List<ResourceInfo> getDirectoryContent(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userName =userDetails.getUsername();
        return directoryService.getDirectoryContent(userName, path);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfo createDirectory(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userName =userDetails.getUsername();
        return directoryService.createDirectory(userName, path);
    }
}