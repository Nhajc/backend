package com.nhajc.server.comment.service;
import com.nhajc.server.comment.dto.*;
import com.nhajc.server.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface CommentService {
    PageResponse<CommentResponse> list(UUID actor,UUID song,Pageable page);
    PageResponse<CommentResponse> replies(UUID actor,UUID parent,Pageable page);
    CommentResponse create(UUID actor,UUID song,CommentRequest request);
    CommentResponse reply(UUID actor,UUID parent,CommentRequest request);
    void delete(UUID id,UUID actorId,boolean admin);
}

