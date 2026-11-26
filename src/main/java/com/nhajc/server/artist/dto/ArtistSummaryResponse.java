package com.nhajc.server.artist.dto;

import java.time.Instant;
import java.util.UUID;

public record ArtistSummaryResponse(UUID id, String name, String avatarUrl, Instant followedAt) {}
