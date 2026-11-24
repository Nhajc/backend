package com.nhajc.server.release.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderSongsRequest(@NotNull List<@NotNull UUID> songIds) {}
