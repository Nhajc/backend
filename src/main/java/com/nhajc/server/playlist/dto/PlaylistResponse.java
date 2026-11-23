package com.nhajc.server.playlist.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;
import com.nhajc.server.song.dto.SongResponse;
public record PlaylistResponse(@JsonUnwrapped PlaylistSummaryResponse summary, List<SongResponse> songs) {}

