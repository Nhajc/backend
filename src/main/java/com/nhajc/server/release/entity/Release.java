package com.nhajc.server.release.entity;

import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.common.entity.UuidEntity;
import com.nhajc.server.genre.entity.Genre;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "releases")
public class Release extends UuidEntity {
    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_type", nullable = false, length = 20)
    private ReleaseType releaseType;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "cover_object_key", columnDefinition = "text")
    private String coverObjectKey;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "release_artists",
            joinColumns = @JoinColumn(name = "release_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id"))
    @OrderColumn(name = "position")
    private List<Artist> artists = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "release_genres",
            joinColumns = @JoinColumn(name = "release_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new LinkedHashSet<>();

    protected Release() {}

    public Release(
            UUID id,
            String title,
            ReleaseType type,
            LocalDate date,
            String cover,
            List<Artist> artists,
            Set<Genre> genres) {
        super(id);
        this.title = title;
        this.releaseType = type;
        this.releaseDate = date;
        this.coverObjectKey = cover;
        this.artists.addAll(artists);
        this.genres.addAll(genres);
    }

    public String getTitle() {
        return title;
    }

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getCoverObjectKey() {
        return coverObjectKey;
    }

    public List<Artist> getArtists() {
        return Collections.unmodifiableList(artists);
    }

    public void setCoverObjectKey(String key) {
        this.coverObjectKey = key;
    }
}
