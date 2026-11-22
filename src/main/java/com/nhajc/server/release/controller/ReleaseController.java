package com.nhajc.server.release.controller;

import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.release.dto.ReleaseRequest;
import com.nhajc.server.release.service.ReleaseService;
import com.nhajc.server.release.entity.ReleaseType;
import com.nhajc.server.common.validation.MultipartRequests;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/releases")
public class ReleaseController {
    private final ReleaseService releases;
    private final MultipartRequests multipart;
    public ReleaseController(ReleaseService releases, MultipartRequests multipart) { this.releases = releases; this.multipart = multipart; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@RequestParam String title, @RequestParam ReleaseType releaseType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
            @RequestParam List<UUID> artistIds,
            @RequestParam(required = false) List<UUID> genreIds,
            @RequestPart(value = "cover", required = false) MultipartFile cover, HttpServletRequest request) {
        multipart.requireFilePart(request, "cover", cover);
        ReleaseRequest body = new ReleaseRequest(title, releaseType, releaseDate, artistIds,
                genreIds == null ? List.of() : genreIds, cover);
        return new IdResponse(releases.create(multipart.validate(body)));
    }
}
