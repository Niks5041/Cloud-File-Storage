package ru.anikson.cloudfilestorage.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.anikson.cloudfilestorage.service.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity // Включает Spring Security
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService cuds;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http     // Настройка правил доступа
                .csrf(csrf -> csrf.disable()) // Отключаем CSRF, так как у нас REST API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/sign-up", "/api/auth/sign-in").permitAll() // Разрешаем доступ без авторизации к указанным URL
                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Сессия создается только при необходимости
                        .maximumSessions(1) // Один пользователь — одна активная сессия
                )
                // Настройка формы входа
//                .formLogin(login -> login
//                        .loginProcessingUrl("/api/auth/sign-in") // URL, который обрабатывает Spring Security
//                        .successHandler((request, response, authentication) -> {
//                            response.setStatus(HttpServletResponse.SC_OK);
//                            response.getWriter().write("{\"message\": \"Вход выполнен успешно!\"}");
//                            response.getWriter().flush();
//                        })
//                        .failureHandler((request, response, exception) -> {
//                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                            response.getWriter().write("{\"error\": \"Неверные учетные данные\"}");
//                            response.getWriter().flush();
//                        })
//                )
                // Настройка выхода из системы
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout") // URL для выхода
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                            response.getWriter().write("{\"message\": \"Выход выполнен успешно!\"}");
                            response.getWriter().flush();
                        })
                        .invalidateHttpSession(true) // Уничтожаем сессию при выходе
                        .deleteCookies("JSESSIONID") // Удаляем куки сессии
                );

        return http.build(); // Собираем и возвращаем объект SecurityFilterChain
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Настраиваем шифрование паролей с использованием BCrypt
    }
     //Конфигурация аутентификации Spring Security
    //AuthenticationConfiguration автоматически подтягивает нужные настройки аутентификации.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
