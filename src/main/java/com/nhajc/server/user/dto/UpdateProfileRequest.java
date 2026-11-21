package com.nhajc.server.user.dto;

import org.springframework.web.multipart.MultipartFile;

public record UpdateProfileRequest(MultipartFile avatar) {
}
