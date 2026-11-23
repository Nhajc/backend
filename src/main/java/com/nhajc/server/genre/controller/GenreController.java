package com.nhajc.server.genre.controller;

import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.pagination.PageRequests;
import com.nhajc.server.genre.dto.GenreRequest;
import com.nhajc.server.genre.dto.GenreResponse;
import com.nhajc.server.genre.service.GenreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class GenreController {
    private final GenreService genres;
    public GenreController(GenreService genres) { this.genres = genres; }

    @PostMapping("/admin/genres") @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@Valid @RequestBody GenreRequest request) { return new IdResponse(genres.create(request)); }

    @GetMapping("/genres")
    public PageResponse<GenreResponse> list(@RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) int size) {
        return genres.list(PageRequests.of(page, size));
    }
}
