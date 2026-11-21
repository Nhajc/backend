package com.nhajc.server.user.service;

import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse updateProfile(String subject, UpdateProfileRequest request);

    void migrateLegacyAvatar(UUID userId);
}
