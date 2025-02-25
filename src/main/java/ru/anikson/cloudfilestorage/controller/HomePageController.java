package ru.anikson.cloudfilestorage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @GetMapping("/api/home")
    public String showHomePage() {
        return "index.html";
    }
}
