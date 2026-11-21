package com.nhajc.server.user.dto;

import com.nhajc.server.user.entity.User;
import com.nhajc.server.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Instant createdAt,
        UserRole role,
        String avatar
) {
    public static UserResponse from(User user, String avatar) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getCreatedAt(), user.getRole(), avatar);
    }
}
