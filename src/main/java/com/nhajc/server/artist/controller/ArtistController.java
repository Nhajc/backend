package com.nhajc.server.artist.controller;

import com.nhajc.server.artist.dto.ArtistRequest;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.common.validation.MultipartRequests;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/artists")
public class ArtistController {
    private final ArtistService artists;
    private final MultipartRequests multipart;
    public ArtistController(ArtistService artists, MultipartRequests multipart) { this.artists = artists; this.multipart = multipart; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@RequestParam String name,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar, HttpServletRequest request) {
        multipart.requireFilePart(request, "avatar", avatar);
        return new IdResponse(artists.create(multipart.validate(new ArtistRequest(name, avatar))));
    }
}
