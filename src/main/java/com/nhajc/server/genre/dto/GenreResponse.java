package com.nhajc.server.genre.dto;

import com.nhajc.server.genre.entity.Genre;
import java.util.UUID;

public record GenreResponse(UUID id, String name, String slug) {
    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName(), genre.getSlug());
    }
}
