package com.nhajc.server.song.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;

public record SongResponse(
        @JsonUnwrapped SongSummaryResponse summary, UUID releaseId, String releaseTitle, String coverUrl) {}
