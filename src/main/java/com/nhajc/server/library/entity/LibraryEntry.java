package com.nhajc.server.library.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

@MappedSuperclass
public abstract class LibraryEntry implements Persistable<LibraryId> {
    @EmbeddedId private LibraryId id;

    @Column(name = "last_activity_at", nullable = false)
    private Instant activityAt;

    @Transient private boolean newEntity = true;

    protected LibraryEntry() {}

    protected LibraryEntry(LibraryId id) {
        this.id = id;
        touch();
    }

    @Override
    public LibraryId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void touch() {
        activityAt = Instant.now();
    }

    @PostLoad
    @PostPersist
    protected void markPersisted() {
        newEntity = false;
    }
}
