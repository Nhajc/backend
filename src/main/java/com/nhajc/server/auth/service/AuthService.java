package com.nhajc.server.auth.service;

import com.nhajc.server.auth.dto.AuthResponse;
import com.nhajc.server.auth.dto.LoginRequest;
import com.nhajc.server.auth.dto.RefreshTokenRequest;
import com.nhajc.server.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
