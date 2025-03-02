package ru.anikson.cloudfilestorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.anikson.cloudfilestorage.dto.UserResponse;
import ru.anikson.cloudfilestorage.entity.User;
import ru.anikson.cloudfilestorage.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@RequestBody User user, HttpServletRequest request) {
        log.info("POST /api/auth/sign-up");
        return authService.registerAndAuthenticate(user, request);
    }

    @PostMapping("/sign-in")
    public UserResponse login(@RequestBody User user, HttpServletRequest request) {
        log.info("POST /api/auth/sign-in");
        return authService.authenticateUser(user, request);
    }

    @PostMapping("/sign-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody User user, HttpServletRequest request) {
        log.info("POST /api/auth/sign-out");
    }
}
