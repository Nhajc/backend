package com.nhajc.server.artist.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;
import com.nhajc.server.release.dto.ReleaseSummaryResponse;
public record ArtistResponse(@JsonUnwrapped ArtistSummaryResponse summary,
                             List<ReleaseSummaryResponse> releases) {}

