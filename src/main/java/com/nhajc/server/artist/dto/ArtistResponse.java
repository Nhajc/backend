package com.nhajc.server.artist.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nhajc.server.release.dto.ReleaseSummaryResponse;
import java.util.List;

public record ArtistResponse(@JsonUnwrapped ArtistSummaryResponse summary, List<ReleaseSummaryResponse> releases) {}
