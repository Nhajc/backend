package com.nhajc.server.song.controller;

import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.common.validation.MultipartRequests;
import jakarta.servlet.http.HttpServletRequest;
import com.nhajc.server.song.dto.UploadSongRequest;
import com.nhajc.server.song.service.SongService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SongController {
    private final SongService songs;
    private final MultipartRequests multipart;
    public SongController(SongService songs, MultipartRequests multipart) { this.songs=songs;this.multipart=multipart; }

    @PostMapping(value = "/admin/releases/{releaseId}/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse upload(@PathVariable UUID releaseId,
            @RequestPart("data") @Valid @NotNull UploadSongRequest data,
            @RequestPart("file") MultipartFile file,HttpServletRequest request) {
        multipart.validateParts(request);
        return new IdResponse(songs.upload(releaseId,data,file));
    }

    @GetMapping("/songs/{id}/audio/{quality}")
    public ResponseEntity<Void> audio(@PathVariable UUID id, @PathVariable String quality) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(songs.audioUrl(id, quality))).build();
    }
}
