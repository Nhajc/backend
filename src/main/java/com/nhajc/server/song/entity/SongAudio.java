package com.nhajc.server.song.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "song_audio")
public class SongAudio {
    @EmbeddedId private SongAudioId id;

    @MapsId("songId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "object_key", nullable = false, unique = true, columnDefinition = "text")
    private String objectKey;

    protected SongAudio() {}

    public SongAudio(Song song, String quality, String objectKey) {
        this.id = new SongAudioId(song.getId(), quality);
        this.song = song;
        this.objectKey = objectKey;
    }

    public String getObjectKey() {
        return objectKey;
    }
}
