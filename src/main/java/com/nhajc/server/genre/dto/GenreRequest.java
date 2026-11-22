package com.nhajc.server.genre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreRequest(@NotBlank @Size(max = 100) String name) {
    public GenreRequest { name = name == null ? null : name.strip(); }
}
