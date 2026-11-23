package com.nhajc.server.artist.dto;
import java.util.UUID;
import java.time.Instant;
public record ArtistSummaryResponse(UUID id, String name, String avatarUrl, Instant followedAt) {}

