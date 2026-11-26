package com.nhajc.server.library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_favorite_songs")
@AttributeOverrides({
    @AttributeOverride(name = "id.itemId", column = @Column(name = "song_id")),
    @AttributeOverride(name = "activityAt", column = @Column(name = "favorited_at", nullable = false))
})
public class FavoriteSong extends LibraryEntry {
    protected FavoriteSong() {}

    public FavoriteSong(LibraryId id) {
        super(id);
    }
}
