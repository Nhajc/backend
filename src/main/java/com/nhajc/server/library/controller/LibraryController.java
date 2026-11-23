package com.nhajc.server.library.controller;
import com.nhajc.server.library.service.LibraryService;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.pagination.PageRequests;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/library")
public class LibraryController {
    private final LibraryService library;
    public LibraryController(LibraryService library) { this.library=library; }
    @GetMapping({"","/{type}"})
    public PageResponse<?> list(@PathVariable(required=false) String type,
            @RequestParam(required=false) String q, @RequestParam(defaultValue="RECENT") LibrarySort sort,
            @RequestParam(defaultValue="false") boolean owned, @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) int size, @AuthenticationPrincipal Jwt jwt) {
        return library.list(Actors.id(jwt),type==null?null:ItemType.fromPath(type),q,sort,owned,PageRequests.of(page,size));
    }
    @PutMapping("/{type}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable String type,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        library.save(Actors.id(jwt),ItemType.fromPath(type),id);
    }
    @DeleteMapping("/{type}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String type,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        library.remove(Actors.id(jwt),ItemType.fromPath(type),id);
    }
}

