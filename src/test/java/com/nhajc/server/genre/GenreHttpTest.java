package com.nhajc.server.genre;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.http.HttpTestSupport;
import com.nhajc.server.genre.dto.GenreResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;

class GenreHttpTest extends HttpTestSupport {
    @Test
    void adminCreationReturnsIdObject() throws Exception {
        UUID id = UUID.randomUUID();
        when(genres.create(any())).thenReturn(id);
        mvc.perform(
                        post("/api/v1/admin/genres")
                                .header("Authorization", "Bearer " + token("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Pop\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void genreListUsesDefaultPageAndResponseEnvelope() throws Exception {
        UUID id = UUID.randomUUID();
        when(genres.list(any()))
                .thenReturn(new PageResponse<>(List.of(new GenreResponse(id, "Pop", "pop")), 0, 20, true));
        mvc.perform(get("/api/v1/genres").header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.hasNext").value(true));
        verify(genres).list(argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20));
    }

    @Test
    void genreListBindsExplicitPaginationAndEmptyPage() throws Exception {
        when(genres.list(any())).thenReturn(new PageResponse<>(List.of(), 3, 5, false));
        mvc.perform(
                        get("/api/v1/genres")
                                .param("page", "3")
                                .param("size", "5")
                                .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
        verify(genres)
                .list(argThat(page -> page.getPageNumber() == 3 && page.getPageSize() == 5 && page.getOffset() == 15));
    }

    @Test
    void badPaginationReturns400BeforeCallingService() throws Exception {
        for (String[] params : new String[][] {{"-1", "20"}, {"0", "0"}, {"0", "101"}, {"text", "20"}, {"0", "text"}}) {
            mvc.perform(
                            get("/api/v1/genres")
                                    .param("page", params[0])
                                    .param("size", params[1])
                                    .header("Authorization", "Bearer " + token("USER")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
        verifyNoInteractions(genres);
    }
}
