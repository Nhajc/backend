package com.nhajc.server.release;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhajc.server.common.http.HttpTestSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ReleaseHttpTest extends HttpTestSupport {
    @Test
    void bindsJsonArraysDateAndOptionalCoverFile() throws Exception {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), id = UUID.randomUUID();
        when(releases.create(any(), any())).thenReturn(id);
        var cover = new MockMultipartFile("file", "cover.png", "image/png", new byte[] {1, 2});
        String json =
                "{\"title\":\" Album \",\"releaseType\":\"ALBUM\",\"releaseDate\":\"2026-11-23\",\"artistIds\":[\""
                        + first
                        + "\",\""
                        + second
                        + "\"]}";
        mvc.perform(
                        multipart("/api/v1/admin/releases")
                                .file(data(json))
                                .file(cover)
                                .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isCreated());
        verify(releases)
                .create(
                        argThat(r ->
                                r.title().equals("Album")
                                        && r.artistIds().equals(List.of(first, second))
                                        && r.genreIds().isEmpty()),
                        eq(cover));
    }

    @Test
    void rejectsMissingArtistsAndNonMultipartBody() throws Exception {
        mvc.perform(
                        multipart("/api/v1/admin/releases")
                                .file(
                                        data(
                                                "{\"title\":\"Album\",\"releaseType\":\"ALBUM\",\"releaseDate\":\"2026-11-23\"}"))
                                .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/admin/releases")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(releases);
    }
}
