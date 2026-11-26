package com.nhajc.server.release.dto;

import com.nhajc.server.release.entity.ReleaseType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReleaseRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull ReleaseType releaseType,
        @NotNull LocalDate releaseDate,
        @NotNull @Size(min = 1, max = 20) List<@NotNull UUID> artistIds,
        @NotNull @Size(max = 20) List<@NotNull UUID> genreIds) {
    public ReleaseRequest {
        title = title == null ? null : title.strip();
        genreIds = genreIds == null ? List.of() : genreIds;
    }
}
