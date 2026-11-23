package com.nhajc.server.user.service;
import org.springframework.web.multipart.MultipartFile;

import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.dto.UserResponse;


public interface UserService {
    UserResponse me(String subject);

    UserResponse updateProfile(String subject, UpdateProfileRequest request, MultipartFile file);
}
