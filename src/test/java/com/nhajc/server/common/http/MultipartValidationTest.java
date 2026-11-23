package com.nhajc.server.common.http;
import com.nhajc.server.common.http.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class MultipartValidationTest extends HttpTestSupport {
    @Test void validatesJsonRatherThanOldTextMetadata() throws Exception {
        mvc.perform(multipart("/api/v1/admin/artists").file(data("{\"name\":\"  \"}"))
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/admin/artists").param("name","Artist")
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/admin/artists").file(new MockMultipartFile("data","","text/plain","{}".getBytes()))
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isUnsupportedMediaType());
        mvc.perform(multipart("/api/v1/admin/artists").file(data("null"))
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(artists);
    }
}
