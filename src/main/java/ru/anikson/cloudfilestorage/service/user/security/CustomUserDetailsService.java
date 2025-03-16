package ru.anikson.cloudfilestorage.service.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.anikson.cloudfilestorage.dao.UserRepository;
import ru.anikson.cloudfilestorage.entity.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Метод, который Spring Security вызывает при аутентификации пользователя по логину.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername()) // Устанавливаем логин пользователя.
                .password(user.getPassword()) // Устанавливаем пароль (зашифрованный).
                .build();
    }// Создаем объект UserDetails, который Spring Security использует для проверки учетных данных.
}

