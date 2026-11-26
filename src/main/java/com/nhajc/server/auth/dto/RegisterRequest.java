package com.nhajc.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
                @Size(min = 3, max = 50)
                @Pattern(regexp = "(?U)[^@\\s]+", message = "must not contain @ or whitespace")
                String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password) {
    public RegisterRequest {
        username = username == null ? null : username.strip();
        email = email == null ? null : email.strip();
    }
}
