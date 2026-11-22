package com.nhajc.server.user.controller;

import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return users.me(jwt.getSubject());
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                      @RequestPart(value = "avatar", required = false) MultipartFile avatar,
                                      HttpServletRequest request) {
        if (avatar == null && request.getParameterMap().containsKey("avatar")) {
            // An unreadable or text-only avatar must not be treated as avatar removal.
            log.warn("Profile update contains an avatar text field without a file part");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid avatar part. Send avatar as a File in multipart/form-data, not as text or a file path.");
        }
        return users.updateProfile(jwt.getSubject(), new UpdateProfileRequest(avatar));
    }
}
