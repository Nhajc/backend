package com.nhajc.server.artist.controller;
import com.nhajc.server.artist.service.ArtistReadService;
import com.nhajc.server.artist.dto.ArtistResponse;
import com.nhajc.server.common.security.Actors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/artists")
public class ArtistReadController {
    private final ArtistReadService artists;
    public ArtistReadController(ArtistReadService artists) { this.artists=artists; }
    @GetMapping("/{id}")
    public ArtistResponse get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        return artists.get(Actors.id(jwt),id);
    }
}

