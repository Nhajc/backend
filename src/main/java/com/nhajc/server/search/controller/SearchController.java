package com.nhajc.server.search.controller;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.pagination.PageRequests;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.library.entity.ItemType;
import com.nhajc.server.search.service.SearchService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService search;
    public SearchController(SearchService search) { this.search=search; }
    @GetMapping({"","/{type}"})
    public PageResponse<?> search(@PathVariable(required=false) String type, @RequestParam String q,
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) int page, @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return search.search(Actors.id(jwt),type==null?null:ItemType.fromPath(type),q,PageRequests.of(page,size));
    }
}

