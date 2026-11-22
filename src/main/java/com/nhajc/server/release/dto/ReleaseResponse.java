package com.nhajc.server.release.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.song.dto.SongSummaryResponse;
public record ReleaseResponse(@JsonUnwrapped ReleaseSummaryResponse summary, PageResponse<SongSummaryResponse> songs) {}

