package ru.anikson.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikson.cloudfilestorage.dao.UserRepository;
import ru.anikson.cloudfilestorage.entity.User;
import ru.anikson.cloudfilestorage.exception.NotFoundException;
import ru.anikson.cloudfilestorage.exception.ValidationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(String username, String password) {
        log.info("Регистрация пользователя {}", username);
        if (userRepository.existsByUsername(username)) {
            throw new ValidationException("Пользователь с таким именем уже существует");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));

        userRepository.save(newUser);
        log.info("Пользователь {} зарегистрирован", username);
    }

    @Override
    public UserDetails authenticateUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        // Возвращаем объект UserDetails, который используется Spring Security для аутентификации
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .build();

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

