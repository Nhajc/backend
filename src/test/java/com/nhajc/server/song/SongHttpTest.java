package com.nhajc.server.song;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhajc.server.common.http.HttpTestSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SongHttpTest extends HttpTestSupport {
    @Test
    void acceptsJsonMetadataWithRequiredFlacFile() throws Exception {
        UUID id = UUID.randomUUID();
        when(songs.upload(any(), any(), any())).thenReturn(id);
        var file = new MockMultipartFile("file", "song.flac", "audio/flac", new byte[] {1, 2});
        mvc.perform(
                        multipart("/api/v1/admin/releases/" + UUID.randomUUID() + "/songs")
                                .file(data("{\"trackNumber\":1,\"title\":\" Song \"}"))
                                .file(file)
                                .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isCreated());
        verify(songs).upload(any(), argThat(r -> r.trackNumber() == 1 && r.title().equals("Song")), eq(file));
    }

    @Test
    void rejectsInvalidMetadataAndMissingFile() throws Exception {
        String path = "/api/v1/admin/releases/" + UUID.randomUUID() + "/songs";
        mvc.perform(
                        multipart(path)
                                .file(data("{\"trackNumber\":0,\"title\":\"Song\"}"))
                                .file(new MockMultipartFile("file", new byte[] {1}))
                                .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(path)
                                .file(data("{\"trackNumber\":1,\"title\":\"Song\"}"))
                                .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(songs);
    }
}
