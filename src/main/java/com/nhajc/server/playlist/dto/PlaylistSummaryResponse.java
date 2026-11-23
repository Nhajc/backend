package com.nhajc.server.playlist.dto;
import com.nhajc.server.user.dto.UserSummaryResponse;
import java.util.UUID;
import java.time.Instant;
public record PlaylistSummaryResponse(UUID id, String title, String coverUrl, boolean isPrivate,
                                      UserSummaryResponse owner, long songCount, Instant lastActivityAt) {}

