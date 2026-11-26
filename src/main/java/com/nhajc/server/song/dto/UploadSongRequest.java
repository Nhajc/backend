package com.nhajc.server.song.dto;

import jakarta.validation.constraints.*;

public record UploadSongRequest(@NotNull @Min(1) Integer trackNumber, @NotBlank @Size(max = 255) String title) {
    public UploadSongRequest {
        title = title == null ? null : title.strip();
    }
}
