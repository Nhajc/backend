package com.nhajc.server.library.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_saved_releases")
@AttributeOverrides({
    @AttributeOverride(name = "id.itemId", column = @Column(name = "release_id")),
    @AttributeOverride(name = "activityAt", column = @Column(name = "last_activity_at", nullable = false))
})
public class SavedRelease extends LibraryEntry {
    protected SavedRelease() {}

    public SavedRelease(LibraryId id) {
        super(id);
    }
}
