package ru.anikson.cloudfilestorage.service;

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
        log.info("Учетные данные пользователя {} проверены", user.getUsername(), user.getPassword());

        // Устанавливаем аутентификацию в SecurityContext чтобы Spring Security знал, что у сессии есть аутентифицированный пользователь
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        // Создаем сессию и сохраняем SecurityContext
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        log.info("Сессия создана для пользователя {}: {}", user.getUsername(), session.getId());
        log.info("Сессия содержит атрибут SPRING_SECURITY_CONTEXT: {}", session.getAttribute("SPRING_SECURITY_CONTEXT"));

        log.info("Пользователь {} успешно вошел в систему", user.getUsername());
        return new UserResponse(user.getUsername());
    }
}


