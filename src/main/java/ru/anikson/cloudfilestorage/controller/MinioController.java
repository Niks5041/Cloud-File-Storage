package ru.anikson.cloudfilestorage.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.anikson.cloudfilestorage.service.MinioService;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/files")
public class MinioController {

    private final MinioService minioService;

    // Метод для отображения списка файлов в выбранной папке
    @GetMapping
    public String listFiles(@RequestParam(value = "folder", defaultValue = "") String folder, Model model,
                            @AuthenticationPrincipal UserDetails userDetails) throws Exception { //Анотация  позволяет  получить текущего авторизованного пользователя
        // Проверка, авторизован ли пользователь
        if (userDetails == null || !userDetails.isEnabled()) {
            log.warn("Попытка доступа к файлам неавторизованного пользователя");
            return "redirect:/login";
        }

        log.info("Пользователь {} запросил список файлов в папке: {}", userDetails.getUsername(), folder);
        // Получаем список файлов в указанной папке
        List<String> files = minioService.listFiles(folder);
        // Добавляем список файлов и текущую папку в модель
        model.addAttribute("files", files);
        model.addAttribute("folder", folder);
        log.info("Количество файлов в папке {}: {}", folder, files.size());
        // Возвращаем название шаблона для отображения
        return "file-list";
    }

    // Метод для загрузки файла
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("folder") String folder,
                             @RequestParam("file") MultipartFile file,
                             @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        // Проверка авторизации
        if (userDetails == null || !userDetails.isEnabled()) {
            log.warn("Попытка загрузки файла неавторизованным пользователем");
            return "redirect:/login";  // Перенаправляем на страницу логина
        }

        log.info("Пользователь {} загрузил файл: {} в папку: {}", userDetails.getUsername(), file.getOriginalFilename(), folder);
        // Загружаем файл с использованием MinioService
        minioService.uploadFile(folder, file);
        // После успешной загрузки перенаправляем обратно в текущую папку
        log.info("Файл {} успешно загружен в папку: {}", file.getOriginalFilename(), folder);
        return "redirect:/files?folder=" + folder;
    }

    // Метод для скачивания файла
    @GetMapping("/download")
    public void downloadFile(@RequestParam("file") String file,
                             HttpServletResponse response,
                             @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        // Проверка авторизации
        if (userDetails == null || !userDetails.isEnabled()) {
            log.warn("Попытка скачивания файла неавторизованным пользователем");
            response.sendRedirect("/login");  // Перенаправляем на страницу логина
            return;
        }

        log.info("Пользователь {} запросил скачивание файла: {}", userDetails.getUsername(), file);
        // Получаем поток данных для скачивания файла
        InputStream fileStream = minioService.downloadFile(file);
        // Устанавливаем заголовки для ответа, чтобы браузер скачал файл
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file + "\"");
        // Передаем данные файла в ответ
        fileStream.transferTo(response.getOutputStream());
        log.info("Файл {} передан для скачивания", file);
    }

    // Метод для удаления файла
    @PostMapping("/delete")
    public String deleteFile(@RequestParam("file") String file, @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        // Проверка авторизации
        if (userDetails == null || !userDetails.isEnabled()) {
            log.warn("Попытка удаления файла неавторизованным пользователем");
            return "redirect:/login";  // Перенаправляем на страницу логина
        }

        log.info("Пользователь {} запросил удаление файла: {}", userDetails.getUsername(), file);
        // Удаляем файл с помощью MinioService
        minioService.deleteFile(file);
        log.info("Файл {} успешно удален", file);
        // Перенаправляем на страницу со списком файлов после удаления
        return "redirect:/files";
    }

    // Метод для переименования файла
    @PostMapping("/rename")
    public String renameFile(@RequestParam("oldName") String oldName,
                             @RequestParam("newName") String newName,
                             @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        // Проверка авторизации
        if (userDetails == null || !userDetails.isEnabled()) {
            log.warn("Попытка переименования файла неавторизованным пользователем");
            return "redirect:/login";  // Перенаправляем на страницу логина
        }

        log.info("Пользователь {} запросил переименование файла: {} в {}", userDetails.getUsername(), oldName, newName);
        // Переименовываем файл с помощью MinioService
        minioService.renameFile(oldName, newName);
        log.info("Файл {} успешно переименован в {}", oldName, newName);
        // Перенаправляем на страницу со списком файлов после переименования
        return "redirect:/files";
    }
}
