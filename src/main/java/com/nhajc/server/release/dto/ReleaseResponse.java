package com.nhajc.server.release.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nhajc.server.song.dto.SongSummaryResponse;
import java.util.List;

public record ReleaseResponse(@JsonUnwrapped ReleaseSummaryResponse summary, List<SongSummaryResponse> songs) {}
