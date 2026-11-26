package com.nhajc.server.playlist.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nhajc.server.song.dto.SongResponse;
import java.util.List;

public record PlaylistResponse(@JsonUnwrapped PlaylistSummaryResponse summary, List<SongResponse> songs) {}
