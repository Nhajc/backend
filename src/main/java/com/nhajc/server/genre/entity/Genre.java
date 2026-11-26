package com.nhajc.server.genre.entity;

import com.nhajc.server.common.entity.UuidEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "genres")
public class Genre extends UuidEntity {
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    protected Genre() {}

    public Genre(UUID id, String name, String slug) {
        super(id);
        this.name = name;
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }
}
