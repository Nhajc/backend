package com.nhajc.server.admin.controller;

import com.nhajc.server.admin.service.StagedSongUploadService;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.validation.MultipartRequests;
import com.nhajc.server.song.dto.SaveUploadedSongsRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/releases/{releaseId}/song-uploads")
public class StagedSongUploadController {
    private final StagedSongUploadService uploads;
    private final MultipartRequests multipart;
    public StagedSongUploadController(StagedSongUploadService uploads, MultipartRequests multipart) { this.uploads=uploads; this.multipart=multipart; }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public StagedSongUploadService.UploadedSong upload(@PathVariable UUID releaseId,@RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,HttpServletRequest request) {
        multipart.validateParts(request);
        return uploads.upload(Actors.id(jwt),releaseId,file);
    }
    @PutMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable UUID releaseId,@RequestBody @Valid SaveUploadedSongsRequest request,@AuthenticationPrincipal Jwt jwt) {
        uploads.save(Actors.id(jwt),releaseId,request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@PathVariable UUID releaseId,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        uploads.discard(Actors.id(jwt),releaseId,id);
    }
}
