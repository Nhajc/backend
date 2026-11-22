package com.nhajc.server.artist.entity;

import com.nhajc.server.common.entity.UuidEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "artists")
public class Artist extends UuidEntity {
    @Column(nullable = false, columnDefinition = "text")
    private String name;
    @Column(name = "avatar_object_key", columnDefinition = "text")
    private String avatarObjectKey;

    protected Artist() {}
    public Artist(UUID id, String name, String avatarObjectKey) {
        super(id); this.name = name; this.avatarObjectKey = avatarObjectKey;
    }
    public String getName() { return name; }
    public String getAvatarObjectKey() { return avatarObjectKey; }
}
