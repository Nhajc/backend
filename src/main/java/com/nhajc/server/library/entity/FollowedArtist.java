package com.nhajc.server.library.entity;
import jakarta.persistence.*;
@Entity @Table(name="user_followed_artists")
@AttributeOverrides({
    @AttributeOverride(name="id.itemId", column=@Column(name="artist_id")),
    @AttributeOverride(name="activityAt", column=@Column(name="followed_at",nullable=false))
})
public class FollowedArtist extends LibraryEntry {
    protected FollowedArtist() {}
    public FollowedArtist(LibraryId id) { super(id); }
}

