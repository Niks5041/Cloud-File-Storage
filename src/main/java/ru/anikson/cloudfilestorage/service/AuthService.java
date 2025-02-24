package ru.anikson.cloudfilestorage.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikson.cloudfilestorage.dao.UserRepository;
import ru.anikson.cloudfilestorage.dto.UserResponse;
import ru.anikson.cloudfilestorage.entity.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserResponse registerAndAuthenticate(User user, HttpServletRequest request) {
        log.info("Регистрация пользователя {}", user.getUsername());

        if (userRepository.existsByUsername(user.getUsername())) {
            log.warn("Пользователь {} уже существует", user.getUsername());
            throw new ValidationException("Пользователь с таким именем уже существует!");
        }

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(newUser);
        log.info("Пользователь {} зарегистрирован", user.getUsername());

        return authenticateUser(newUser, request);
    }

    public UserResponse authenticateUser(User user, HttpServletRequest request) {
        log.info("Аутентификация пользователя {}", user.getUsername());

        // Проверяем учетные данные пользователя
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );

        // Устанавливаем пользователя в контекст безопасности Spring Security
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Создаем сессию, если ее нет (true означает, что сессия будет создана)
        request.getSession(true);

        log.info("Пользователь {} успешно вошел в систему", user.getUsername());
        return new UserResponse(user.getUsername());
    }
}


