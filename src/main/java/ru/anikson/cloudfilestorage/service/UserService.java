package ru.anikson.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikson.cloudfilestorage.dao.UserRepository;
import ru.anikson.cloudfilestorage.entity.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean registerUser(String username, String password) {
        log.info("Регистрация пользователя {}", username);
        if (userRepository.existsByUsername(username)) {
            log.warn("Пользователь {} уже существует", username);
           return false;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));

        userRepository.save(newUser);
        log.info("Пользователь {} зарегистрирован", username);
        return true;
    }

//    public UserDetails getCurrentUser() {
//        // Получаем текущего пользователя из контекста безопасности
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//
//        // Проверяем, является ли principal экземпляром UserDetails
//        if (principal instanceof UserDetails) {
//            // Если да, возвращаем объект UserDetails (это интерфейс, который предоставляет информацию о пользователе)
//            return (UserDetails) principal;
//        }
//
//        // Если principal не является объектом UserDetails, значит, пользователь не аутентифицирован
//        return null;
//    }
}

