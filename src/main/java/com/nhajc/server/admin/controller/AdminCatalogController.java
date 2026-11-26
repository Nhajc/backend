package com.nhajc.server.admin.controller;

import com.nhajc.server.admin.service.AdminCatalogService;
import com.nhajc.server.artist.dto.ArtistSummaryResponse;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.pagination.PageRequests;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.release.dto.ReleaseSummaryResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final AdminCatalogService catalog;

    public AdminCatalogController(AdminCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/artists")
    public PageResponse<ArtistSummaryResponse> artists(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return catalog.artists(Actors.id(jwt), q, PageRequests.of(page, size));
    }

    @GetMapping("/releases")
    public PageResponse<ReleaseSummaryResponse> releases(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return catalog.releases(Actors.id(jwt), q, PageRequests.of(page, size));
    }
}
