package ru.anikson.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.anikson.cloudfilestorage.entity.User;
import ru.anikson.cloudfilestorage.service.UserService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        log.info("GET /register");
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String registerUser(@ModelAttribute User user, RedirectAttributes attributes) {
        log.info("POST /register");
        userService.registerUser(user.getUsername(), user.getPassword());
        attributes.addFlashAttribute("message", "Регистрация успешна!");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        log.info("GET /login");
        model.addAttribute("loginForm", new User());
        return "login";
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public String loginUser(@ModelAttribute User user, RedirectAttributes attributes) {
        log.info("POST /login: {}", user.getUsername());

        UserDetails authenticated = userService.authenticateUser(user.getUsername());

        if (authenticated) {
            attributes.addFlashAttribute("message", "Вход успешен!");
            return "redirect:/cloud";
        } else {
            attributes.addFlashAttribute("error", "Неверные учетные данные");
            return "redirect:/login";
        }
    }

    @GetMapping("/cloud")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /cloud");
        if (userDetails != null) {
            log.info("Аутентифицированный пользователь: {}", userDetails.getUsername());
        }
        model.addAttribute("username", userDetails.getUsername());
        return "profile";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login";
    }
}
