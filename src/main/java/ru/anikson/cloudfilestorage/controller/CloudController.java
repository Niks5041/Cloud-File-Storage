package ru.anikson.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CloudController {

    @GetMapping("/cloud")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /cloud");
        if (userDetails.isEnabled()) {
            log.info("Пользователь: {} авторизован", userDetails);
        }
        model.addAttribute("username", userDetails.getUsername());
        return "cloud";
    }
}
