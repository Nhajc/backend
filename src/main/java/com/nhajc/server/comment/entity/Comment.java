package com.nhajc.server.comment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import com.nhajc.server.common.entity.UuidEntity;

@Entity
@Table(name = "comments")
public class Comment extends UuidEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "song_id", nullable = false)
    private UUID songId;
    @Column(name = "parent_id")
    private UUID parentId;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(name = "commented_at", nullable = false)
    private Instant commentedAt;

    protected Comment() {}
    public Comment(UUID id, UUID userId, UUID songId, UUID parentId, String content) {
        super(id); this.userId=userId; this.songId=songId; this.parentId=parentId;
        this.content=content; this.commentedAt=Instant.now();
    }
    public UUID getUserId() { return userId; }
    public UUID getSongId() { return songId; }
    public UUID getParentId() { return parentId; }
    public String getContent() { return content; }
    public Instant getCommentedAt() { return commentedAt; }
}
