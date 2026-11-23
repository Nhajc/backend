package com.nhajc.server.release.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;
import com.nhajc.server.song.dto.SongSummaryResponse;
public record ReleaseResponse(@JsonUnwrapped ReleaseSummaryResponse summary, List<SongSummaryResponse> songs) {}

