package ru.anikson.cloudfilestorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
                        .requestMatchers("/", "/register").permitAll() // Разрешаем доступ без авторизации к указанным URL
                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                )
                // Настройка формы входа
                .formLogin(login -> login
                        .loginPage("/login") // Указываем страницу кастомного логина, что пользователь должен заходить через /login, а не стандартную страницу Spring Security.
                        .defaultSuccessUrl("/cloud", true) // Параметр true указывает, что этот URL должен быть использован в качестве стандартного  После успешного входа перенаправляем на страницу профиля
                        .permitAll() // Разрешает всем видеть страницу логина, даже если они не авторизованы.
                )
                // Настройка выхода из системы
                .logout(logout -> logout
                        .logoutUrl("/logout") // URL для выхода из системы
                        .logoutSuccessUrl("/") // После выхода перенаправляем на главную страницу
                        .permitAll() // Разрешаем выход всем
                );

        return http.build(); // Собираем и возвращаем объект SecurityFilterChain
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Настраиваем шифрование паролей с использованием BCrypt
    }
}
