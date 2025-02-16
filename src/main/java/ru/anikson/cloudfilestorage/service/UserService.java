package ru.anikson.cloudfilestorage.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserService {

    void registerUser(String username, String password);

    UserDetails authenticateUser(String username);

    //UserDetails getCurrentUser();
}
