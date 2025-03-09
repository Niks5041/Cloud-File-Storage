package ru.anikson.cloudfilestorage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.anikson.cloudfilestorage.dto.UserResponse;
import ru.anikson.cloudfilestorage.exception.NotFoundException;

@RestController
@RequestMapping("/api/users/me")
@Slf4j
public class MeController {

    @GetMapping
    public UserResponse getMySelfAgain(@AuthenticationPrincipal UserDetails ud) {
        log.info("GET api/users/me");
        if (!ud.isEnabled()) {
            log.error("Пользователь не найден");
            throw new NotFoundException("Пользователь не найден");
        }
        return new UserResponse(ud.getUsername());
    }
}
