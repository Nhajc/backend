package com.nhajc.server.common.http;

import com.nhajc.server.artist.controller.ArtistController;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.common.exception.ApiExceptionHandler;
import com.nhajc.server.common.validation.MultipartRequests;
import com.nhajc.server.config.SecurityConfig;
import com.nhajc.server.genre.controller.GenreController;
import com.nhajc.server.genre.service.GenreService;
import com.nhajc.server.release.controller.ReleaseController;
import com.nhajc.server.release.service.ReleaseService;
import com.nhajc.server.song.controller.SongController;
import com.nhajc.server.song.service.SongService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class) @WebAppConfiguration
@ContextConfiguration(classes = HttpTestSupport.Config.class)
@TestPropertySource(properties = "JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
public abstract class HttpTestSupport {
    @Autowired protected WebApplicationContext context;
    @Autowired protected GenreService genres;
    @Autowired protected ArtistService artists;
    @Autowired protected ReleaseService releases;
    @Autowired protected SongService songs;
    @Autowired protected com.nhajc.server.playlist.service.PlaylistService playlists;
    protected MockMvc mvc;
    @BeforeEach protected void setUp() {
        reset(genres, artists, releases, songs, playlists);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    protected MockMultipartFile data(String json) {
        return new MockMultipartFile("data","","application/json",json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    protected String token(String role) {
        Instant now = Instant.now();
        return context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("nhajc").subject(UUID.randomUUID().toString()).issuedAt(now)
                        .expiresAt(now.plusSeconds(60)).claim("role", role).build())).getTokenValue();
    }
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, GenreController.class, ArtistController.class, ReleaseController.class,
            SongController.class, com.nhajc.server.playlist.controller.PlaylistController.class, ApiExceptionHandler.class, MultipartRequests.class})
    public static class Config {
        @Bean GenreService genres() { return mock(GenreService.class); }
        @Bean ArtistService artists() { return mock(ArtistService.class); }
        @Bean ReleaseService releases() { return mock(ReleaseService.class); }
        @Bean SongService songs() { return mock(SongService.class); }
        @Bean com.nhajc.server.playlist.service.PlaylistService playlists() {return mock(com.nhajc.server.playlist.service.PlaylistService.class);}
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }
}
