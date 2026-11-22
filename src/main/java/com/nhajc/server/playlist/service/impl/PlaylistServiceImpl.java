package com.nhajc.server.playlist.service.impl;
import com.nhajc.server.playlist.service.PlaylistService;
import com.nhajc.server.playlist.dto.*;
import com.nhajc.server.playlist.entity.*;
import com.nhajc.server.playlist.repository.*;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.library.repository.SavedPlaylistRepository;
import com.nhajc.server.common.service.*;
import com.nhajc.server.common.dto.PageResponse;
import com.nhajc.server.common.pagination.PageRequests;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class PlaylistServiceImpl implements PlaylistService {
    private final PlaylistRepository playlists;
    private final PlaylistSongRepository entries;
    private final SongRepository songs;
    private final SavedPlaylistRepository saved;
    private final UserAccess users;
    private final MediaViews views;
    public PlaylistServiceImpl(PlaylistRepository playlists, PlaylistSongRepository entries, SongRepository songs,
            SavedPlaylistRepository saved, UserAccess users, MediaViews views) {
        this.playlists=playlists;this.entries=entries;this.songs=songs;this.saved=saved;this.users=users;this.views=views;
    }
    @Override @Transactional
    public PlaylistResponse get(UUID actor, UUID id, Pageable pageable) {
        var page=PageRequests.of(pageable.getPageNumber(),pageable.getPageSize());
        users.lock(actor);
        Playlist playlist=playlists.findById(id).orElseThrow(PlaylistServiceImpl::missing);
        if(playlist.isPrivate()&&!playlist.getOwner().getId().equals(actor))throw missing();
        saved.findById(new LibraryId(actor,id)).ifPresent(LibraryEntry::touch);
        return response(actor,playlist,page);
    }
    @Override @Transactional
    public PlaylistResponse create(UUID actor, PlaylistRequest request) {
        var owner=users.lock(actor);
        if(playlists.countByOwner_Id(actor)>=200)throw new ResponseStatusException(HttpStatus.CONFLICT,"You can own at most 200 playlists.");
        var playlist=playlists.saveAndFlush(new Playlist(UUID.randomUUID(),request.title(),request.isPrivate(),owner));
        saved.saveAndFlush(new SavedPlaylist(new LibraryId(actor,playlist.getId())));
        return response(actor,playlist,PageRequests.of(0,20));
    }
    @Override @Transactional public void update(UUID actor,UUID id,PlaylistRequest request) {
        owned(actor,id).update(request.title(),request.isPrivate());
    }
    @Override @Transactional public void delete(UUID actor,UUID id) { playlists.delete(owned(actor,id)); }
    @Override @Transactional public void addSong(UUID actor,UUID id,UUID songId) {
        var playlist=owned(actor,id);
        var key=new PlaylistSongId(id,songId);
        if(entries.existsById(key))return;
        var song=songs.findById(songId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Song not found."));
        if(entries.countById_PlaylistId(id)>=1000)throw new ResponseStatusException(HttpStatus.CONFLICT,"Playlist can contain at most 1000 songs.");
        entries.save(new PlaylistSong(playlist,song,entries.lastPosition(id)+1));
    }
    @Override @Transactional public void removeSong(UUID actor,UUID id,UUID songId) {
        owned(actor,id); entries.deleteById(new PlaylistSongId(id,songId));
    }
    @Override @Transactional public void reorder(UUID actor,UUID id,List<UUID> songIds) {
        owned(actor,id);
        if(songIds==null||songIds.size()>1000||songIds.stream().anyMatch(Objects::isNull)||new HashSet<>(songIds).size()!=songIds.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide each song ID exactly once.");
        // This bounded write operation loads at most the playlist's 1000 rows.
        var existing=entries.findById_PlaylistIdOrderByPositionAsc(id);
        var byId=new HashMap<UUID,PlaylistSong>();existing.forEach(entry->byId.put(entry.getId().getSongId(),entry));
        if(!byId.keySet().equals(new HashSet<>(songIds)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reorder must contain exactly the playlist's songs.");
        // V1's position uniqueness is DEFERRABLE, so swaps are checked at commit.
        for(int i=0;i<songIds.size();i++)byId.get(songIds.get(i)).setPosition(i);
    }
    private Playlist owned(UUID actor,UUID id) {
        users.lock(actor);
        var p=playlists.findLockedById(id).orElseThrow(PlaylistServiceImpl::missing);
        if(!p.getOwner().getId().equals(actor)) {
            if(p.isPrivate())throw missing();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the playlist owner can modify it.");
        }
        return p;
    }
    private PlaylistResponse response(UUID actor,Playlist playlist,Pageable page) {
        var slice=entries.findById_PlaylistIdOrderByPositionAsc(playlist.getId(),page);
        var mapped=views.songs(slice.getContent().stream().map(entry->entry.getId().getSongId()).toList(),actor);
        var summary=views.playlists(List.of(playlist.getId()),actor).get(playlist.getId());
        return new PlaylistResponse(summary,PageResponse.from(slice,entry->mapped.get(entry.getId().getSongId())));
    }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Playlist not found."); }
}

