package com.nhajc.server.artist.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.release.dto.ReleaseSummaryResponse;
public record ArtistResponse(@JsonUnwrapped ArtistSummaryResponse summary, boolean isFollowed,
                             PageResponse<ReleaseSummaryResponse> releases) {}

