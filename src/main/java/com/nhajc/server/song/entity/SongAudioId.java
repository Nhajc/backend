package com.nhajc.server.song.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SongAudioId implements Serializable {
    @Column(name = "song_id")
    private UUID songId;
    @Column(length = 20)
    private String quality;

    protected SongAudioId() {}
    public SongAudioId(UUID songId, String quality) { this.songId = songId; this.quality = quality; }
    @Override public boolean equals(Object other) {
        return other instanceof SongAudioId id && Objects.equals(songId, id.songId) && Objects.equals(quality, id.quality);
    }
    @Override public int hashCode() { return Objects.hash(songId, quality); }
}
