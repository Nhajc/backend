package com.nhajc.server.playlist.entity;
import com.nhajc.server.common.entity.UuidEntity;
import com.nhajc.server.user.entity.User;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="playlists")
public class Playlist extends UuidEntity {
    @Column(nullable=false, columnDefinition="text") private String title;
    @Column(name="is_private", nullable=false) private boolean isPrivate;
    @Column(name="cover_object_key", columnDefinition="text") private String coverObjectKey;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id", nullable=false) private User owner;
    protected Playlist() {}
    public Playlist(UUID id, String title, boolean isPrivate, User owner) {
        super(id); this.title=title; this.isPrivate=isPrivate; this.owner=owner;
    }
    public String getTitle() { return title; }
    public boolean isPrivate() { return isPrivate; }
    public User getOwner() { return owner; }
    public String getCoverObjectKey() { return coverObjectKey; }
    public void setCoverObjectKey(String coverObjectKey) { this.coverObjectKey = coverObjectKey; }
    public void update(String title, boolean isPrivate) { this.title=title; this.isPrivate=isPrivate; }
}

