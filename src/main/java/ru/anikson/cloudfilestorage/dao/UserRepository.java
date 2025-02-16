package ru.anikson.cloudfilestorage.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.anikson.cloudfilestorage.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}
