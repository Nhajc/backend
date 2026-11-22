package com.nhajc.server.song.controller;

import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.song.dto.UploadSongRequest;
import com.nhajc.server.song.service.SongService;
import jakarta.validation.Validator;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SongController {
    private final SongService songs;
    private final Validator validator;
    public SongController(SongService songs, Validator validator) { this.songs = songs; this.validator = validator; }

    @PostMapping(value = "/admin/releases/{releaseId}/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse upload(@PathVariable UUID releaseId, @RequestParam Integer trackNumber,
            @RequestParam String title, @RequestPart("file") MultipartFile file) {
        UploadSongRequest request = new UploadSongRequest(trackNumber, title, file);
        if (!validator.validate(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Track number must be at least 1; title is required and must be at most 255 characters.");
        }
        return new IdResponse(songs.upload(releaseId, request));
    }

    @GetMapping("/songs/{id}/audio/{quality}")
    public ResponseEntity<Void> audio(@PathVariable UUID id, @PathVariable String quality) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(songs.audioUrl(id, quality))).build();
    }
}
