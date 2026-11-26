package com.nhajc.server.song.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record SaveUploadedSongsRequest(
        @NotEmpty List<@NotNull @Valid SongDetails> songs, @NotNull List<@NotNull UUID> songIds) {
    public record SongDetails(
            @NotNull UUID id,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String description,
            @NotEmpty @Size(max = 20) List<@NotNull UUID> artistIds) {
        public SongDetails {
            title = title == null ? null : title.strip();
            description = description == null || description.isBlank() ? null : description.strip();
        }
    }
}
