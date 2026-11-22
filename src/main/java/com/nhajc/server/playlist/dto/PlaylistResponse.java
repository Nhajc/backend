package com.nhajc.server.playlist.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.song.dto.SongResponse;
public record PlaylistResponse(@JsonUnwrapped PlaylistSummaryResponse summary, PageResponse<SongResponse> songs) {}

