package com.nhajc.server.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@MappedSuperclass
public abstract class UuidEntity implements Persistable<UUID> {
    @Id private UUID id;

    @Transient private boolean newEntity = true;

    protected UuidEntity() {}

    protected UuidEntity(UUID id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    protected void markPersisted() {
        newEntity = false;
    }
}
