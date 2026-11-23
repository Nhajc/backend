package com.nhajc.server.artist;
import com.nhajc.server.common.http.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ArtistHttpTest extends HttpTestSupport {
    @Test void acceptsJsonDataAndOptionalFile() throws Exception {
        UUID id=UUID.randomUUID();when(artists.create(any(),any())).thenReturn(id);
        var file=new MockMultipartFile("file","avatar.png","image/png",new byte[]{1,2});
        mvc.perform(multipart("/api/v1/admin/artists").file(data("{\"name\":\" Artist \"}")).file(file)
                .header("Authorization","Bearer "+token("ADMIN")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        verify(artists).create(argThat(r->r.name().equals("Artist")),eq(file));
    }
    @Test void optionalFileCanBeOmittedButTextFileIsRejected() throws Exception {
        mvc.perform(multipart("/api/v1/admin/artists").file(data("{\"name\":\"Artist\"}"))
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isCreated());
        verify(artists).create(any(),isNull());reset(artists);
        mvc.perform(multipart("/api/v1/admin/artists").file(data("{\"name\":\"Artist\"}")).param("file","C:/image.png")
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(artists);
    }
}
