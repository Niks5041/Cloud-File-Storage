package ru.anikson.cloudfilestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.anikson.cloudfilestorage.entity.User;
import ru.anikson.cloudfilestorage.service.UserService;
import ru.anikson.cloudfilestorage.service.security.CustomUserDetailsService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        log.info("GET /register");
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    //@ResponseStatus(HttpStatus.CREATED)
    public String registerUser(@ModelAttribute User user, RedirectAttributes attributes) {
        log.info("POST /register");
        boolean registrationCheckUser = userService.registerUser(user.getUsername(), user.getPassword());
        if (!registrationCheckUser) {
            log.error("Пользователь с таким именем уже существует");
            attributes.addFlashAttribute("message", "Пользователь с таким именем уже существует!");
            return "redirect:/register";
        }
        log.info("Регистрация прошла успешна!");
        return  "redirect:/login";
    }

//    @GetMapping("/login")
//    public String showLoginForm(Model model) {
//        log.info("GET /login");
//        model.addAttribute("loginForm", new User());
//        return "login";
//    }
}
