package com.nhajc.server.playlist.service.impl;
import org.springframework.web.multipart.MultipartFile;

import com.nhajc.server.playlist.service.PlaylistService;
import com.nhajc.server.playlist.dto.*;
import com.nhajc.server.playlist.entity.*;
import com.nhajc.server.playlist.repository.*;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.library.entity.*;
import com.nhajc.server.library.repository.SavedPlaylistRepository;
import com.nhajc.server.common.service.*;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.storage.ObjectStorageCleanup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class PlaylistServiceImpl implements PlaylistService {
    private final PlaylistRepository playlists;
    private final PlaylistSongRepository entries;
    private final SongRepository songs;
    private final SavedPlaylistRepository saved;
    private final UserAccess users;
    private final MediaViews views;
    private final ImageStorage images;
    private final ObjectStorageCleanup cleanup;
    private final TransactionTemplate transaction;

    public PlaylistServiceImpl(PlaylistRepository playlists, PlaylistSongRepository entries, SongRepository songs,
                               SavedPlaylistRepository saved, UserAccess users, MediaViews views,
                               ImageStorage images, ObjectStorageCleanup cleanup, PlatformTransactionManager manager) {
        this.playlists = playlists;
        this.entries = entries;
        this.songs = songs;
        this.saved = saved;
        this.users = users;
        this.views = views;
        this.images = images;
        this.cleanup = cleanup;
        this.transaction = new TransactionTemplate(manager);
    }

    @Override
    @Transactional
    public PlaylistResponse get(UUID actor, UUID id) {
        users.require(actor);
        Playlist playlist = playlists.findById(id).orElseThrow(PlaylistServiceImpl::missing);
        if (playlist.isPrivate() && !playlist.getOwner().getId().equals(actor)) throw missing();
        saved.touch(actor, id, Instant.now());
        return response(actor, playlist);
    }

    @Override
    public PlaylistResponse create(UUID actor, PlaylistRequest request, MultipartFile file) {
        users.require(actor);
        requireUnique(request.songIds());
        UUID id = UUID.randomUUID();
        String key = images.upload("covers/playlists/" + id + "/cover", images.fromMultipart(file));
        return transaction.execute(status -> {
            var owner = users.lock(actor);
            if (playlists.countByOwner_Id(actor) >= 200) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You can own at most 200 playlists.");
            }
            var playlist = new Playlist(id, request.title(), request.isPrivate(), owner);
            playlist.setCoverObjectKey(key);
            playlists.saveAndFlush(playlist);
            saved.saveAndFlush(new SavedPlaylist(new LibraryId(actor, id)));
            replaceSongs(playlist, request.songIds());
            cleanup.completeUpload(key);
            return response(actor, playlist);
        });
    }

    @Override
    public void update(UUID actor, UUID id, PlaylistRequest request, MultipartFile file) {
        users.require(actor);
        requireUnique(request.songIds());
        transaction.executeWithoutResult(status -> requireOwner(actor, playlists.findById(id)));
        String key = images.upload("covers/playlists/" + id + "/cover", images.fromMultipart(file));
        transaction.executeWithoutResult(status -> {
            var playlist = owned(actor, id);
            playlist.update(request.title(), request.isPrivate());
            if (request.isPrivate()) saved.deleteOthers(id, actor);
            replaceSongs(playlist, request.songIds());
            if (key != null) {
                cleanup.enqueue(playlist.getCoverObjectKey());
                playlist.setCoverObjectKey(key);
                cleanup.completeUpload(key);
            }
        });
    }

    @Override
    @Transactional
    public void delete(UUID actor, UUID id) {
        var playlist = owned(actor, id);
        cleanup.enqueue(playlist.getCoverObjectKey());
        playlists.delete(playlist);
    }

    @Override
    @Transactional
    public void addSong(UUID actor, UUID id, UUID songId) {
        var playlist = owned(actor, id);
        var key = new PlaylistSongId(id, songId);
        if (entries.existsById(key)) return;
        var song = songs.findById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found."));
        if (entries.countById_PlaylistId(id) >= 1000) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Playlist can contain at most 1000 songs.");
        }
        entries.save(new PlaylistSong(playlist, song, entries.lastPosition(id) + 1));
    }

    private void replaceSongs(Playlist playlist, List<UUID> songIds) {
        var byId = new HashMap<UUID, Song>();
        songs.findAllById(songIds).forEach(song -> byId.put(song.getId(), song));
        if (byId.size() != songIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found.");
        }
        entries.deleteByPlaylist(playlist.getId());
        for (int position = 0; position < songIds.size(); position++) {
            entries.save(new PlaylistSong(playlist, byId.get(songIds.get(position)), position));
        }
    }

    private static void requireUnique(List<UUID> songIds) {
        if (new HashSet<>(songIds).size() != songIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Song IDs must not be repeated.");
        }
    }

    private Playlist owned(UUID actor, UUID id) {
        users.require(actor);
        return requireOwner(actor, playlists.findLockedById(id));
    }

    private static Playlist requireOwner(UUID actor, Optional<Playlist> found) {
        var playlist = found.orElseThrow(PlaylistServiceImpl::missing);
        if (!playlist.getOwner().getId().equals(actor)) {
            if (playlist.isPrivate()) throw missing();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the playlist owner can modify it.");
        }
        return playlist;
    }

    private PlaylistResponse response(UUID actor, Playlist playlist) {
        var values = entries.findById_PlaylistIdOrderByPositionAsc(playlist.getId());
        var mapped = views.songs(values.stream().map(entry -> entry.getId().getSongId()).toList(), actor);
        var summary = views.playlists(List.of(playlist.getId()), actor).get(playlist.getId());
        return new PlaylistResponse(summary, values.stream().map(entry -> mapped.get(entry.getId().getSongId())).toList());
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found.");
    }
}
