package com.nhajc.server.library.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;
@Embeddable
public class LibraryId implements Serializable {
    @Column(name="user_id") private UUID userId;
    @Column(name="item_id") private UUID itemId;
    protected LibraryId() {}
    public LibraryId(UUID userId, UUID itemId) { this.userId=userId; this.itemId=itemId; }
    public UUID getUserId() { return userId; }
    public UUID getItemId() { return itemId; }
    @Override public boolean equals(Object value) { return value instanceof LibraryId other && Objects.equals(userId,other.userId) && Objects.equals(itemId,other.itemId); }
    @Override public int hashCode() { return Objects.hash(userId,itemId); }
}

