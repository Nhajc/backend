package com.nhajc.server.artist.controller;
import com.nhajc.server.artist.dto.ArtistRequest;
import com.nhajc.server.artist.dto.ArtistResponse;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.validation.MultipartRequests;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1")
public class ArtistController {
    private final ArtistService artists;
    private final MultipartRequests multipart;
    public ArtistController(ArtistService artists, MultipartRequests multipart) { this.artists=artists;this.multipart=multipart; }
    @PostMapping(value="/admin/artists",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@RequestPart("data") @Valid @NotNull ArtistRequest data,
            @RequestPart(value="file",required=false) MultipartFile file,HttpServletRequest request) {
        multipart.validateParts(request);
        return new IdResponse(artists.create(data,file));
    }
    @GetMapping("/artists/{id}")
    public ArtistResponse get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        return artists.get(Actors.id(jwt),id);
    }
}
