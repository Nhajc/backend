package com.nhajc.server.song.entity;

import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.common.entity.UuidEntity;
import com.nhajc.server.release.entity.Release;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "songs", uniqueConstraints = @UniqueConstraint(columnNames = {"release_id", "track_number"}))
public class Song extends UuidEntity {
    @Column(nullable = false, columnDefinition = "text")
    private String title;
    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;
    @Column(name = "track_number", nullable = false)
    private int trackNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "song_artists", joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id"))
    private Set<Artist> artists = new LinkedHashSet<>();
    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SongAudio> audio = new LinkedHashSet<>();

    protected Song() {}
    public Song(UUID id, String title, int duration, int track, Release release, Map<String, String> keys) {
        super(id); this.title = title; this.durationSeconds = duration; this.trackNumber = track;
        this.release = release; this.artists.addAll(release.getArtists());
        keys.forEach((quality, key) -> audio.add(new SongAudio(this, quality, key)));
    }
    public String getTitle() { return title; }
    public Release getRelease() { return release; }
    public Set<Artist> getArtists() { return Collections.unmodifiableSet(artists); }
    public int getDurationSeconds() { return durationSeconds; }
    public int getTrackNumber() { return trackNumber; }
    public Set<SongAudio> getAudio() { return Collections.unmodifiableSet(audio); }
}
