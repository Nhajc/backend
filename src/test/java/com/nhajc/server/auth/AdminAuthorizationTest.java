package com.nhajc.server.auth;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nhajc.server.common.http.HttpTestSupport;

class AdminAuthorizationTest extends HttpTestSupport {
    @Test void userIsDeniedAllAdminWrites() throws Exception {
        for (String path : new String[]{"/genres", "/artists", "/releases"}) {
            mvc.perform(post("/api/v1/admin" + path).header("Authorization", "Bearer " + token("USER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(multipart("/api/v1/admin/releases/" + UUID.randomUUID() + "/songs")
                .file(new MockMultipartFile("file", new byte[]{1})).header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(genres, artists, releases, songs);
    }
}
