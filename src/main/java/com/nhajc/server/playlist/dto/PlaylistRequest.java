package com.nhajc.server.playlist.dto;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record PlaylistRequest(@NotBlank @Size(max=255) String title, @NotNull Boolean isPrivate,
                              @NotNull @Size(max=1000) List<@NotNull UUID> songIds) {
    public PlaylistRequest { title = title == null ? null : title.strip(); }
}

