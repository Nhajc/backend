package com.nhajc.server.artist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record ArtistRequest(@NotBlank @Size(max = 255) String name, MultipartFile avatar) {
    public ArtistRequest { name = name == null ? null : name.strip(); }
}
