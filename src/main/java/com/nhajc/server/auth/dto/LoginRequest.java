package com.nhajc.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) String usernameOrEmail,
        @NotBlank String password
) {
    public LoginRequest {
        usernameOrEmail = usernameOrEmail == null ? null : usernameOrEmail.strip();
    }
}
