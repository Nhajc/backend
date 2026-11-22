package com.nhajc.server.catalog;
import com.nhajc.server.common.dto.*;
import com.nhajc.server.artist.dto.*;
import com.nhajc.server.playlist.dto.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ResponseContractsTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void mixedItemsKeepDiscriminatorAndFlattenWithinGenericPage() {
        var dto=new TypedItemResponse<>("ARTIST",new ArtistSummaryResponse(UUID.randomUUID(),"Artist",null));
        var json=mapper.readTree(mapper.writeValueAsString(new PageResponse<>(List.of(dto),2,5,true)));
        assertThat(json.get("page").asInt()).isEqualTo(2);
        assertThat(json.get("size").asInt()).isEqualTo(5);
        var item=json.get("content").get(0);
        assertThat(item.get("type").asText()).isEqualTo("ARTIST");
        assertThat(item.get("name").asText()).isEqualTo("Artist");
        assertThat(item.has("item")).isFalse();
        assertThat(item.get("avatarUrl").isNull()).isTrue();
    }
    @Test void playlistBooleansAndNestedPagesMatchTheWireContract() {
        var summary=new PlaylistSummaryResponse(UUID.randomUUID(),"Test",null,true,null,0,true);
        var dto=new PlaylistResponse(summary,new PageResponse<>(List.of(),4,10,false));
        var json=mapper.readTree(mapper.writeValueAsString(dto));
        assertThat(json.get("isPrivate").asBoolean()).isTrue();
        assertThat(json.get("isSaved").asBoolean()).isTrue();
        assertThat(json.has("private")).isFalse();
        assertThat(json.has("saved")).isFalse();
        assertThat(json.has("summary")).isFalse();
        assertThat(json.get("songs").get("page").asInt()).isEqualTo(4);
        assertThat(json.get("songs").get("size").asInt()).isEqualTo(10);
    }
}

