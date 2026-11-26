package com.nhajc.server.playlist;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhajc.server.common.http.HttpTestSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PlaylistHttpTest extends HttpTestSupport {
    @Test
    void createUsesJsonBooleanAndCoverFile() throws Exception {
        var file = new MockMultipartFile("file", "cover.png", "image/png", new byte[] {1});
        mvc.perform(
                        multipart("/api/v1/playlists")
                                .file(data("{\"title\":\" New \",\"isPrivate\":false,\"songIds\":[]}"))
                                .file(file)
                                .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isCreated());
        verify(playlists).create(any(), argThat(r -> r.title().equals("New") && !r.isPrivate()), eq(file));
    }

    @Test
    void replacementWithoutFileKeepsCover() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(
                        multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/playlists/" + id)
                                .file(data("{\"title\":\"Title\",\"isPrivate\":true,\"songIds\":[]}"))
                                .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isNoContent());
        verify(playlists).update(any(), eq(id), any(), isNull());
    }

    @Test
    void missingPrivacyMissingSongsAndUnexpectedLegacyPartsAreRejected() throws Exception {
        mvc.perform(
                        multipart("/api/v1/playlists")
                                .file(data("{\"title\":\"Title\",\"songIds\":[]}"))
                                .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart("/api/v1/playlists")
                                .file(data("{\"title\":\"Title\",\"isPrivate\":true}"))
                                .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart("/api/v1/playlists")
                                .file(data("{\"title\":\"Title\",\"isPrivate\":true,\"songIds\":[]}"))
                                .file(new MockMultipartFile("cover", "cover.png", "image/png", new byte[] {1}))
                                .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(playlists);
    }
}
