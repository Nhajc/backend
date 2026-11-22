package com.nhajc.server.release.controller;
import com.nhajc.server.release.service.ReleaseReadService;
import com.nhajc.server.release.dto.ReleaseResponse;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.pagination.PageRequests;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/releases")
public class ReleaseReadController {
    private final ReleaseReadService releases;
    public ReleaseReadController(ReleaseReadService releases) { this.releases=releases; }
    @GetMapping("/{id}")
    public ReleaseResponse get(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,@AuthenticationPrincipal Jwt jwt) {
        return releases.get(Actors.id(jwt),id,PageRequests.of(page,size));
    }
}

