package com.nhajc.server.comment.controller;
import com.nhajc.server.comment.service.CommentService;
import com.nhajc.server.comment.dto.*;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.security.Actors;
import com.nhajc.server.common.pagination.PageRequests;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1")
public class CommentController {
    private final CommentService comments;
    public CommentController(CommentService comments) { this.comments=comments; }
    @GetMapping("/songs/{id}/comments")
    public PageResponse<CommentResponse> list(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,@AuthenticationPrincipal Jwt jwt) {
        return comments.list(Actors.id(jwt),id,PageRequests.of(page,size));
    }
    @GetMapping("/comments/{id}/replies")
    public PageResponse<CommentResponse> replies(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,@AuthenticationPrincipal Jwt jwt) {
        return comments.replies(Actors.id(jwt),id,PageRequests.of(page,size));
    }
    @PostMapping("/songs/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable UUID id,@Valid @RequestBody CommentRequest request,@AuthenticationPrincipal Jwt jwt) {
        return comments.create(Actors.id(jwt),id,request);
    }
    @PostMapping("/comments/{id}/replies") @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse reply(@PathVariable UUID id,@Valid @RequestBody CommentRequest request,@AuthenticationPrincipal Jwt jwt) {
        return comments.reply(Actors.id(jwt),id,request);
    }
    @DeleteMapping("/comments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,Authentication authentication) {
        boolean admin=authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
        comments.delete(id,Actors.id(jwt),admin);
    }
}

