package com.nhajc.server.user.dto;
import java.util.UUID;
public record UserSummaryResponse(UUID id, String username, String avatarUrl) {}

