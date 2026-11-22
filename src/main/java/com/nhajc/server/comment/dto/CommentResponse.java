package com.nhajc.server.comment.dto;
import com.nhajc.server.user.dto.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;
public record CommentResponse(UUID id,String content,Instant commentedAt,UserSummaryResponse user,long replyCount) {}

