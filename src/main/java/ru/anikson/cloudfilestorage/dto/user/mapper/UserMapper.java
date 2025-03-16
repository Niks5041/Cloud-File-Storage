package ru.anikson.cloudfilestorage.dto.user.mapper;

import lombok.experimental.UtilityClass;
import ru.anikson.cloudfilestorage.dto.user.UserResponse;
import ru.anikson.cloudfilestorage.entity.User;

@UtilityClass
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getUsername()
        );
    }
}
