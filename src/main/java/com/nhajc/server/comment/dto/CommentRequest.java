package com.nhajc.server.comment.dto;
import jakarta.validation.constraints.*;
public record CommentRequest(@NotBlank @Size(max=2000) String content) {
    public CommentRequest { content=content==null?null:content.strip(); }
}

