package com.nhajc.server.playlist.controller;
import com.nhajc.server.playlist.service.PlaylistService;
import com.nhajc.server.playlist.dto.*;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.pagination.PageRequests;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/playlists")
public class PlaylistController {
    private final PlaylistService playlists;
    public PlaylistController(PlaylistService playlists) { this.playlists=playlists; }
    @GetMapping("/{id}")
    public PlaylistResponse get(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,@AuthenticationPrincipal Jwt jwt) {
        return playlists.get(Actors.id(jwt),id,PageRequests.of(page,size));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PlaylistResponse create(@Valid @RequestBody PlaylistRequest request,@AuthenticationPrincipal Jwt jwt) {
        return playlists.create(Actors.id(jwt),request);
    }
    @PutMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id,@Valid @RequestBody PlaylistRequest request,@AuthenticationPrincipal Jwt jwt) {
        playlists.update(Actors.id(jwt),id,request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { playlists.delete(Actors.id(jwt),id); }
    @PutMapping("/{id}/songs/{songId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable UUID id,@PathVariable UUID songId,@AuthenticationPrincipal Jwt jwt) { playlists.addSong(Actors.id(jwt),id,songId); }
    @DeleteMapping("/{id}/songs/{songId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id,@PathVariable UUID songId,@AuthenticationPrincipal Jwt jwt) { playlists.removeSong(Actors.id(jwt),id,songId); }
    @PutMapping("/{id}/songs") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable UUID id,@RequestBody @NotNull @Size(max=1000) List<@NotNull UUID> songs,@AuthenticationPrincipal Jwt jwt) {
        playlists.reorder(Actors.id(jwt),id,songs);
    }
}

