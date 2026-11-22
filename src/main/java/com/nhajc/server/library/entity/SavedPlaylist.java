package com.nhajc.server.library.entity;
import jakarta.persistence.*;
@Entity @Table(name="user_saved_playlists")
@AttributeOverrides({
    @AttributeOverride(name="id.itemId", column=@Column(name="playlist_id")),
    @AttributeOverride(name="activityAt", column=@Column(name="last_activity_at",nullable=false))
})
public class SavedPlaylist extends LibraryEntry {
    protected SavedPlaylist() {}
    public SavedPlaylist(LibraryId id) { super(id); }
}

