package com.nhajc.server.playlist.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;
@Embeddable
public class PlaylistSongId implements Serializable {
    @Column(name="playlist_id") private UUID playlistId;
    @Column(name="song_id") private UUID songId;
    protected PlaylistSongId() {}
    public PlaylistSongId(UUID playlistId, UUID songId) { this.playlistId=playlistId; this.songId=songId; }
    public UUID getPlaylistId() { return playlistId; }
    public UUID getSongId() { return songId; }
    @Override public boolean equals(Object value) { return value instanceof PlaylistSongId other && Objects.equals(playlistId,other.playlistId) && Objects.equals(songId,other.songId); }
    @Override public int hashCode() { return Objects.hash(playlistId,songId); }
}

