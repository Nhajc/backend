package com.nhajc.server.playlist.dto;
import jakarta.validation.constraints.*;
public record PlaylistRequest(@NotBlank @Size(max=255) String title, @NotNull Boolean isPrivate) {
    public PlaylistRequest { title = title == null ? null : title.strip(); }
}

