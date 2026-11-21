package com.nhajc.server.user.mapper;

import com.nhajc.server.storage.service.ObjectStorageService;
import com.nhajc.server.user.UserAvatarStorage;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {
    private final ObjectStorageService storage;
    private final UserAvatarStorage avatars;

    public UserResponseMapper(ObjectStorageService storage, UserAvatarStorage avatars) {
        this.storage = storage;
        this.avatars = avatars;
    }

    public UserResponse from(User user) {
        if (user.getAvatar() != null) {
            avatars.replace(user, user.getAvatar(), user.getAvatarContentType());
        }
        String url = user.getAvatarObjectKey() == null
                ? null : storage.getPresignedUrl(user.getAvatarObjectKey());
        return UserResponse.from(user, url);
    }
}
