package com.nhajc.server.user.controller;

import com.nhajc.server.auth.service.AuthService;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.service.UserService;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AuthService auth;
    private final UserService users;

    public UserController(AuthService auth, UserService users) {
        this.auth = auth;
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(jwt.getSubject());
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                      @ModelAttribute UpdateProfileRequest request) {
        return users.updateProfile(jwt.getSubject(), request);
    }
}
