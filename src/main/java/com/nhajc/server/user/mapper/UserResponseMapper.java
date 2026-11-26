package com.nhajc.server.user.mapper;

import com.nhajc.server.storage.service.ObjectStorageService;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {
    private final ObjectStorageService storage;

    public UserResponseMapper(ObjectStorageService storage) {
        this.storage = storage;
    }

    public UserResponse from(User user) {
        String url = user.getAvatarObjectKey() == null ? null : storage.getPresignedUrl(user.getAvatarObjectKey());
        return UserResponse.from(user, url);
    }
}
