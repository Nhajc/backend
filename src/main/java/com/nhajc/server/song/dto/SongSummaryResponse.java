package com.nhajc.server.song.dto;
import com.nhajc.server.artist.dto.ArtistSummaryResponse;
import java.util.List;
import java.util.UUID;
public record SongSummaryResponse(UUID id, String title, int durationSeconds, int trackNumber,
                                  List<ArtistSummaryResponse> artists, boolean isFavorited) {}

