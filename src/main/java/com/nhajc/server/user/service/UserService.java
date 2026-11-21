package com.nhajc.server.user.service;

import com.nhajc.server.user.dto.UpdateProfileRequest;
import com.nhajc.server.user.dto.UserResponse;


public interface UserService {
    UserResponse me(String subject);

    UserResponse updateProfile(String subject, UpdateProfileRequest request);
}
