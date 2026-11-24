package com.nhajc.server.playlist.entity;
import com.nhajc.server.song.entity.Song;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
@Entity @Table(name="playlist_songs")
public class PlaylistSong implements Persistable<PlaylistSongId> {
    @EmbeddedId private PlaylistSongId id;
    @MapsId("playlistId") @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="playlist_id") private Playlist playlist;
    @MapsId("songId") @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="song_id") private Song song;
    @Column(nullable=false) private int position;
    @Transient private boolean newEntity=true;
    protected PlaylistSong() {}
    public PlaylistSong(Playlist playlist, Song song, int position) {
        id=new PlaylistSongId(playlist.getId(),song.getId()); this.playlist=playlist; this.song=song; this.position=position;
    }
    @Override public PlaylistSongId getId() { return id; }
    @Override public boolean isNew() { return newEntity; }
    public Song getSong() { return song; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position=position; }
    @PostLoad @PostPersist protected void markPersisted() { newEntity=false; }
}

