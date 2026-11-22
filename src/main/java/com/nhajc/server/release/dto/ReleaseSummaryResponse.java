package com.nhajc.server.release.dto;
import com.nhajc.server.artist.dto.ArtistSummaryResponse;
import com.nhajc.server.release.entity.ReleaseType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record ReleaseSummaryResponse(UUID id, String title, String coverUrl, ReleaseType releaseType,
        LocalDate releaseDate, long songCount, List<ArtistSummaryResponse> artists, boolean isSaved) {}

