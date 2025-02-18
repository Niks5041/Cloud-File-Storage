package ru.anikson.cloudfilestorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Включает Spring Security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Настройка правил доступа
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/home").permitAll() // Разрешаем доступ без авторизации к указанным URL
                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                )
                // Настройка формы входа
                .formLogin(login -> login
                       // .loginPage("/login") // Указываем страницу кастомного логина, что пользователь должен заходить через энодпонит /login, а не стандартную страницу Spring Security.
                        .loginProcessingUrl("/login") // Spring Security сам обрабатывает логин
                        .defaultSuccessUrl("/cloud", true) // Параметр true указывает, что этот URL должен быть использован в качестве стандартного  После успешного входа перенаправляем на страницу профиля
                        .permitAll() // Разрешает всем видеть страницу логина, даже если они не авторизованы.
                )
                // Настройка выхода из системы
                .logout(logout -> logout
                        //.logoutUrl("/logout") // URL для выхода из системы
                        .logoutSuccessUrl("/register") // После выхода перенаправляем на главную страницу
                        .invalidateHttpSession(true) // Уничтожаем сессию при выходе
                        .deleteCookies("JSESSIONID") // Удаляем куки при выходе
                        .permitAll() // Разрешаем выход всем

                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) //   (по умолчанию!) Spring Security создаст сессию только при успешном входе, если ее нет. Если пользователь уже аутентифицирован, Security просто использует текущую сессию
                        .maximumSessions(1) // Только 1 сессия на пользователя
                        .expiredUrl("/login?expired=true") // Если сессия истекла, перенаправляем
                );


        return http.build(); // Собираем и возвращаем объект SecurityFilterChain
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Настраиваем шифрование паролей с использованием BCrypt
    }
}
