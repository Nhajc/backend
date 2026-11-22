package com.nhajc.server.admin;

import com.nhajc.server.artist.controller.ArtistController;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.common.exception.ApiExceptionHandler;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.validation.MultipartRequests;
import com.nhajc.server.genre.dto.GenreResponse;
import com.nhajc.server.config.SecurityConfig;
import com.nhajc.server.genre.controller.GenreController;
import com.nhajc.server.genre.service.GenreService;
import com.nhajc.server.release.controller.ReleaseController;
import com.nhajc.server.release.service.ReleaseService;
import com.nhajc.server.song.controller.SongController;
import com.nhajc.server.song.service.SongService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class) @WebAppConfiguration
@ContextConfiguration(classes = AdminHttpTest.Config.class)
@TestPropertySource(properties = "JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class AdminHttpTest {
    @Autowired WebApplicationContext context;
    @Autowired GenreService genres;
    @Autowired ArtistService artists;
    @Autowired ReleaseService releases;
    @Autowired SongService songs;
    MockMvc mvc;
    @BeforeEach void setUp() {
        reset(genres, artists, releases, songs);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

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

    @Test void adminCreationReturnsIdObject() throws Exception {
        UUID id = UUID.randomUUID(); when(genres.create(any())).thenReturn(id);
        mvc.perform(post("/api/v1/admin/genres").header("Authorization", "Bearer " + token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Pop\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test void invalidBodiesAreRejectedBeforeService() throws Exception {
        mvc.perform(multipart("/api/v1/admin/artists").param("name", "   ")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/admin/releases").param("title", "Album").param("releaseType", "ALBUM")
                        .param("releaseDate", "2026-11-22").header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(artists, releases);
    }

    @Test void artistUsesTextAndOptionalFileParts() throws Exception {
        UUID id = UUID.randomUUID(); when(artists.create(any())).thenReturn(id);
        var avatar = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{1, 2});
        mvc.perform(multipart("/api/v1/admin/artists").param("name", " Artist ").file(avatar)
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        verify(artists).create(argThat(request -> request.name().equals("Artist") && request.avatar() == avatar));
    }

    @Test void artistCanBeCreatedWithoutAvatarButNotWithTextAvatar() throws Exception {
        when(artists.create(any())).thenReturn(UUID.randomUUID());
        mvc.perform(multipart("/api/v1/admin/artists").param("name", "Artist")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isCreated());
        verify(artists).create(argThat(request -> request.avatar() == null));
        reset(artists);
        mvc.perform(multipart("/api/v1/admin/artists").param("name", "Artist").param("avatar", "C:/image.png")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(artists);
    }

    @Test void releaseBindsRepeatedArtistIdsAndOptionalGenresWithCoverFile() throws Exception {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), id = UUID.randomUUID();
        var cover = new MockMultipartFile("cover", "cover.png", "image/png", new byte[]{1, 2});
        when(releases.create(any())).thenReturn(id);
        mvc.perform(multipart("/api/v1/admin/releases").param("title", " Album ").param("releaseType", "ALBUM")
                        .param("releaseDate", "2026-11-22").param("artistIds", first.toString(), second.toString())
                        .file(cover).header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        verify(releases).create(argThat(request -> request.title().equals("Album") && request.cover() == cover
                && request.artistIds().equals(List.of(first, second)) && request.genreIds().isEmpty()));
    }

    @Test void releaseRejectsTextCoverAndJsonBody() throws Exception {
        mvc.perform(multipart("/api/v1/admin/releases").param("title", "Album").param("releaseType", "ALBUM")
                        .param("releaseDate", "2026-11-22").param("artistIds", UUID.randomUUID().toString())
                        .param("cover", "C:/cover.png").header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/releases").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(releases);
    }

    @Test void multipartFileIsReceivedWithValidatedTextFields() throws Exception {
        UUID id = UUID.randomUUID(); when(songs.upload(any(), any())).thenReturn(id);
        mvc.perform(multipart("/api/v1/admin/releases/" + UUID.randomUUID() + "/songs")
                        .file(new MockMultipartFile("file", "song.flac", "audio/flac", new byte[]{1, 2}))
                        .param("trackNumber", "1").param("title", " Song ")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        verify(songs).upload(any(), argThat(request -> request.trackNumber() == 1 && request.title().equals("Song") && request.file().getSize() == 2));
    }

    @Test void badTrackAndMissingFileReturn400() throws Exception {
        String path = "/api/v1/admin/releases/" + UUID.randomUUID() + "/songs";
        mvc.perform(multipart(path).file(new MockMultipartFile("file", new byte[]{1}))
                        .param("trackNumber", "0").param("title", "Song").header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(path).param("trackNumber", "1").param("title", "Song")
                        .header("Authorization", "Bearer " + token("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(songs);
    }

    @Test void genreListUsesDefaultPageAndResponseEnvelope() throws Exception {
        UUID id = UUID.randomUUID();
        when(genres.list(any())).thenReturn(new PageResponse<>(List.of(new GenreResponse(id, "Pop", "pop")), 0, 20, true));
        mvc.perform(get("/api/v1/genres").header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.hasNext").value(true));
        verify(genres).list(argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 20));
    }

    @Test void genreListBindsExplicitPaginationAndEmptyPage() throws Exception {
        when(genres.list(any())).thenReturn(new PageResponse<>(List.of(), 3, 5, false));
        mvc.perform(get("/api/v1/genres").param("page", "3").param("size", "5")
                        .header("Authorization", "Bearer " + token("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
        verify(genres).list(argThat(page -> page.getPageNumber() == 3 && page.getPageSize() == 5 && page.getOffset() == 15));
    }

    @Test void badPaginationReturns400BeforeCallingService() throws Exception {
        for (String[] params : new String[][]{{"-1", "20"}, {"0", "0"}, {"0", "101"}, {"text", "20"}, {"0", "text"}}) {
            mvc.perform(get("/api/v1/genres").param("page", params[0]).param("size", params[1])
                            .header("Authorization", "Bearer " + token("USER")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        verifyNoInteractions(genres);
    }

    private String token(String role) {
        Instant now = Instant.now();
        return context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("nhajc").subject(UUID.randomUUID().toString()).issuedAt(now)
                        .expiresAt(now.plusSeconds(60)).claim("role", role).build())).getTokenValue();
    }
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, GenreController.class, ArtistController.class, ReleaseController.class,
            SongController.class, ApiExceptionHandler.class, MultipartRequests.class})
    static class Config {
        @Bean GenreService genres() { return mock(GenreService.class); }
        @Bean ArtistService artists() { return mock(ArtistService.class); }
        @Bean ReleaseService releases() { return mock(ReleaseService.class); }
        @Bean SongService songs() { return mock(SongService.class); }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }
}
