package com.nhajc.server.common.dto;

import static org.assertj.core.api.Assertions.*;

import com.nhajc.server.artist.dto.*;
import com.nhajc.server.playlist.dto.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ResponseContractsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mixedItemsKeepDiscriminatorAndFlattenWithinGenericPage() {
        var dto = new TypedItemResponse<>("ARTIST", new ArtistSummaryResponse(UUID.randomUUID(), "Artist", null, null));
        var json = mapper.readTree(mapper.writeValueAsString(new PageResponse<>(List.of(dto), 2, 5, true)));
        assertThat(json.get("page").asInt()).isEqualTo(2);
        assertThat(json.get("size").asInt()).isEqualTo(5);
        var item = json.get("content").get(0);
        assertThat(item.get("type").asText()).isEqualTo("ARTIST");
        assertThat(item.get("name").asText()).isEqualTo("Artist");
        assertThat(item.has("item")).isFalse();
        assertThat(item.get("avatarUrl").isNull()).isTrue();
    }

    @Test
    void playlistPrivacyTimestampAndFullSongsMatchTheWireContract() {
        var summary =
                new PlaylistSummaryResponse(
                        UUID.randomUUID(),
                        "Test",
                        null,
                        true,
                        null,
                        0,
                        java.time.Instant.parse("2026-11-23T00:00:00Z"));
        var dto = new PlaylistResponse(summary, List.of());
        var json = mapper.readTree(mapper.writeValueAsString(dto));
        assertThat(json.get("isPrivate").asBoolean()).isTrue();
        assertThat(json.get("lastActivityAt").asText()).isEqualTo("2026-11-23T00:00:00Z");
        assertThat(json.has("isSaved")).isFalse();
        assertThat(json.has("private")).isFalse();
        assertThat(json.has("saved")).isFalse();
        assertThat(json.has("summary")).isFalse();
        assertThat(json.get("songs").isArray()).isTrue();
    }
}
