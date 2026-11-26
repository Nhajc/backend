package com.nhajc.server.auth.dto;

import com.nhajc.server.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}
