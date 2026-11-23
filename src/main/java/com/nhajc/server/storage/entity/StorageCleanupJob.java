package com.nhajc.server.storage.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.Instant;

@Entity
@Table(name = "storage_object_cleanup")
public class StorageCleanupJob implements Persistable<String> {
    @Id
    @Column(name = "object_key", columnDefinition = "text")
    private String objectKey;
    @Column(name = "delete_after", nullable = false)
    private Instant deleteAfter;
    @Column(nullable = false)
    private boolean claimed;
    @Transient
    private boolean newEntity = true;

    protected StorageCleanupJob() {}
    public StorageCleanupJob(String key, Instant deleteAfter) { this.objectKey = key; this.deleteAfter = deleteAfter; }
    public String getObjectKey() { return objectKey; }
    public void claimUntil(Instant retryAfter) {
        claimed = true;
        deleteAfter = retryAfter;
    }
    @Override public String getId() { return objectKey; }
    @Override public boolean isNew() { return newEntity; }
    @PostLoad @PostPersist
    protected void markPersisted() { newEntity = false; }
}
