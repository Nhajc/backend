package com.nhajc.server.admin.service;

import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.repository.SongRepository;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReleaseSongOrderService {
    private final ReleaseRepository releases;
    private final SongRepository songs;
    private final EntityManager entities;

    public ReleaseSongOrderService(ReleaseRepository releases, SongRepository songs, EntityManager entities) {
        this.releases = releases;
        this.songs = songs;
        this.entities = entities;
    }

    @Transactional
    public void reorder(UUID releaseId, List<UUID> songIds) {
        releases.findByIdForUpdate(releaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Release not found."));
        if (songIds == null || songIds.stream().anyMatch(java.util.Objects::isNull) || new HashSet<>(songIds).size() != songIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Song IDs must be present and unique.");
        }
        var current = songs.findByRelease_IdOrderByTrackNumberAscIdAsc(releaseId);
        var byId = current.stream().collect(Collectors.toMap(song -> song.getId(), Function.identity()));
        if (!byId.keySet().equals(new HashSet<>(songIds))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The release songs have changed. Refresh the release before reordering.");
        }
        if (songIds.isEmpty()) return;
        entities.createNativeQuery("SET CONSTRAINTS songs_release_id_track_number_key DEFERRED").executeUpdate();
        for (int index = 0; index < songIds.size(); index++) {
            byId.get(songIds.get(index)).setTrackNumber(index + 1);
        }
        entities.flush();
    }
}
