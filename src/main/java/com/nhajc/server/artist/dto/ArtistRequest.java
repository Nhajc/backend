package com.nhajc.server.artist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtistRequest(@NotBlank @Size(max = 255) String name) {
    public ArtistRequest {
        name = name == null ? null : name.strip();
    }
}
