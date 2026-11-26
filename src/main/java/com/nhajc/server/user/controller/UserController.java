package com.nhajc.server.user.controller;

import com.nhajc.server.common.validation.MultipartRequests;
import com.nhajc.server.user.dto.*;
import com.nhajc.server.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;
    private final MultipartRequests multipart;

    public UserController(UserService users, MultipartRequests multipart) {
        this.users = users;
        this.multipart = multipart;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return users.me(jwt.getSubject());
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("data") @Valid @NotNull UpdateProfileRequest data,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        multipart.validateParts(request);
        return users.updateProfile(jwt.getSubject(), data, file);
    }
}
