package com.nhajc.server.playlist.controller;
import com.nhajc.server.playlist.service.PlaylistService;
import com.nhajc.server.playlist.dto.*;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.validation.MultipartRequests;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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
    private final MultipartRequests multipart;
    public PlaylistController(PlaylistService playlists, MultipartRequests multipart) {
        this.playlists=playlists;
        this.multipart=multipart;
    }
    @GetMapping("/{id}")
    public PlaylistResponse get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        return playlists.get(Actors.id(jwt),id);
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public PlaylistResponse create(@RequestPart("data") @Valid @NotNull PlaylistRequest data,
            @RequestPart(value="file",required=false) MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,HttpServletRequest request) {
        multipart.validateParts(request);
        return playlists.create(Actors.id(jwt),data,file);
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id,@RequestPart("data") @Valid @NotNull PlaylistRequest data,
            @RequestPart(value="file",required=false) MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,HttpServletRequest request) {
        multipart.validateParts(request);
        playlists.update(Actors.id(jwt),id,data,file);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { playlists.delete(Actors.id(jwt),id); }
    @PutMapping("/{id}/songs/{songId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable UUID id,@PathVariable UUID songId,@AuthenticationPrincipal Jwt jwt) { playlists.addSong(Actors.id(jwt),id,songId); }
}

