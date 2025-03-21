package ru.anikson.cloudfilestorage.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikson.cloudfilestorage.dao.UserRepository;
import ru.anikson.cloudfilestorage.dto.user.UserResponse;
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
        log.info("Пользователь зарегистрирован: " + newUser.getUsername(), newUser.getPassword());

        User userForAuth = new User();
        userForAuth.setUsername(user.getUsername());
        userForAuth.setPassword(user.getPassword());

        log.info("Юзер для аутентификации" + userForAuth.getUsername(), user.getPassword());

        return authenticateUser(userForAuth, request);
    }

    public UserResponse authenticateUser(User user, HttpServletRequest request) {
        log.info("Аутентификация пользователя {}", user.getUsername());

        // Проверяем учетные данные пользователя
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );
        log.info("Учетные данные пользователя {} проверены", user.getUsername());

        // Получаем текущий SecurityContext из SecurityContextHolder
// SecurityContextHolder — это глобальное хранилище, которое содержит информацию об аутентификации текущего запроса
        SecurityContext securityContext = SecurityContextHolder.getContext();

// Устанавливаем объект Authentication в SecurityContext
// Authentication содержит информацию о пользователе (имя, роли и т.д.), которая была проверена ранее
// Этот шаг сообщает Spring Security, что пользователь аутентифицирован в рамках текущего запроса
        securityContext.setAuthentication(authentication);

// Создаем новую сессию или получаем существующую через HttpServletRequest
// Параметр true означает, что если сессия еще не существует, она будет создана
// HttpSession используется для хранения данных между запросами в рамках одной сессии пользователя
        HttpSession session = request.getSession(true);

// Явно сохраняем SecurityContext в атрибутах сессии под ключом "SPRING_SECURITY_CONTEXT"
// Это позволяет Spring Security восстановить информацию об аутентификации при следующем запросе
// Обычно Spring Security делает это автоматически через SecurityContextPersistenceFilter, но здесь мы делаем это вручную
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        log.info("Сессия создана для пользователя {}: {}", user.getUsername(), session.getId());

        log.info("Пользователь {} успешно вошел в систему", user.getUsername());
        return new UserResponse(user.getUsername());
    }
}



