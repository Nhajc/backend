package com.nhajc.server.song.dto;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

public record UploadSongRequest(@NotNull @Min(1) Integer trackNumber,
                                @NotBlank @Size(max = 255) String title,
                                @NotNull MultipartFile file) {
    public UploadSongRequest { title = title == null ? null : title.strip(); }
}
