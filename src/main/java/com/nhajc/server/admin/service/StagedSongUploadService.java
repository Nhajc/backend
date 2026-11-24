package com.nhajc.server.admin.service;

import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.dto.SaveUploadedSongsRequest;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.song.service.impl.SongServiceImpl;
import com.nhajc.server.storage.ObjectStorageCleanup;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StagedSongUploadService {
    private final SongServiceImpl audio;
    private final JdbcTemplate jdbc;
    private final ReleaseRepository releases;
    private final SongRepository songs;
    private final ArtistRepository artists;
    private final ObjectStorageCleanup cleanup;
    private final EntityManager entities;
    private final TransactionTemplate transaction;

    public StagedSongUploadService(SongServiceImpl audio, JdbcTemplate jdbc, ReleaseRepository releases,
            SongRepository songs, ArtistRepository artists, ObjectStorageCleanup cleanup,
            EntityManager entities, PlatformTransactionManager manager) {
        this.audio=audio; this.jdbc=jdbc; this.releases=releases; this.songs=songs;
        this.artists=artists; this.cleanup=cleanup; this.entities=entities;
        this.transaction=new TransactionTemplate(manager);
    }

    public record UploadedSong(UUID id, int durationSeconds, Instant expiresAt) {}
    public UploadedSong upload(UUID actor, UUID release, MultipartFile file) {
        var result = new UploadedSong[1];
        audio.prepare(release, file, prepared -> {
            var expires = Instant.now().plusSeconds(3600);
            transaction.executeWithoutResult(status -> jdbc.update("""
                INSERT INTO staged_song_uploads(id,release_id,actor_id,duration_seconds,
                    low_key,medium_key,high_key,lossless_key,cover_key,expires_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, prepared.id(), release, actor, prepared.durationSeconds(), prepared.keys().get("low"),
                    prepared.keys().get("medium"), prepared.keys().get("high"), prepared.keys().get("lossless"),
                    prepared.coverKey(), java.sql.Timestamp.from(expires)));
            result[0]=new UploadedSong(prepared.id(), prepared.durationSeconds(), expires);
            return prepared.id();
        });
        return result[0];
    }

    private record Staged(UUID id, int duration, Map<String,String> keys, String cover) {}
    private Staged requireUpload(UUID actor, UUID release, UUID id) {
        var values = jdbc.query("""
            SELECT * FROM staged_song_uploads
            WHERE id=? AND actor_id=? AND release_id=? AND expires_at>CURRENT_TIMESTAMP FOR UPDATE
            """, (row, number) -> new Staged(row.getObject("id", UUID.class), row.getInt("duration_seconds"),
                    Map.of("low",row.getString("low_key"),"medium",row.getString("medium_key"),
                           "high",row.getString("high_key"),"lossless",row.getString("lossless_key")), row.getString("cover_key")), id,actor,release);
        if (values.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"An upload expired or is unavailable. Refresh the release before retrying.");
        return values.getFirst();
    }

    @Transactional
    public void save(UUID actor, UUID releaseId, SaveUploadedSongsRequest request) {
        var release = releases.findByIdForUpdate(releaseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Release not found."));
        var ids = request.songs().stream().map(SaveUploadedSongsRequest.SongDetails::id).toList();
        if (new HashSet<>(ids).size()!=ids.size() || new HashSet<>(request.songIds()).size()!=request.songIds().size()) throw badInput();
        var current=songs.findByRelease_IdOrderByTrackNumberAscIdAsc(releaseId);
        var allIds=current.stream().map(Song::getId).collect(Collectors.toSet());
        if (ids.stream().anyMatch(allIds::contains)) throw new ResponseStatusException(HttpStatus.CONFLICT,"These songs have already been saved. Refresh the release.");
        allIds.addAll(ids);
        if (!allIds.equals(new HashSet<>(request.songIds()))) throw new ResponseStatusException(HttpStatus.CONFLICT,"The release songs have changed. Refresh before saving.");
        // Validate every upload and every credit before publishing anything.
        var staged = new HashMap<UUID,Staged>();
        var credits = new HashMap<UUID,List<com.nhajc.server.artist.entity.Artist>>();
        for (var detail:request.songs()) {
            staged.put(detail.id(),requireUpload(actor,releaseId,detail.id()));
            if (new HashSet<>(detail.artistIds()).size()!=detail.artistIds().size()) throw badInput();
            var byId=artists.findAllById(detail.artistIds()).stream().collect(Collectors.toMap(a->a.getId(),a->a));
            if (byId.size()!=detail.artistIds().size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Artist not found.");
            credits.put(detail.id(),detail.artistIds().stream().map(byId::get).toList());
        }
        entities.createNativeQuery("SET CONSTRAINTS songs_release_id_track_number_key DEFERRED").executeUpdate();
        for (var song:current) song.setTrackNumber(request.songIds().indexOf(song.getId())+1);
        for (var detail:request.songs()) {
            var upload=staged.get(detail.id());
            var song=new Song(upload.id(),detail.title(),upload.duration(),request.songIds().indexOf(upload.id())+1,release,upload.keys());
            song.setDetails(detail.description(),credits.get(detail.id()));
            songs.save(song);
            upload.keys().values().forEach(cleanup::completeUpload);
            if (upload.cover()!=null && releases.setCoverIfAbsent(releaseId,upload.cover())==1) cleanup.completeUpload(upload.cover());
            jdbc.update("DELETE FROM staged_song_uploads WHERE id=?",upload.id());
        }
        entities.flush();
    }

    @Transactional
    public void discard(UUID actor, UUID release, UUID id) {
        // Same release lock order as save; discarding an already consumed upload is harmless.
        releases.findByIdForUpdate(release).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Release not found."));
        var keys=jdbc.query("SELECT low_key,medium_key,high_key,lossless_key,cover_key FROM staged_song_uploads WHERE id=? AND actor_id=? AND release_id=? FOR UPDATE",
                (row,n)->Arrays.asList(row.getString(1),row.getString(2),row.getString(3),row.getString(4),row.getString(5)),id,actor,release);
        for(var group:keys) for(var key:group) if(key!=null) jdbc.update("UPDATE storage_object_cleanup SET delete_after=CURRENT_TIMESTAMP WHERE object_key=? AND claimed=false",key);
        jdbc.update("DELETE FROM staged_song_uploads WHERE id=? AND actor_id=? AND release_id=?",id,actor,release);
    }

    @Scheduled(fixedDelayString="${storage.cleanup-delay-ms:30000}")
    public void expire() { jdbc.update("DELETE FROM staged_song_uploads WHERE expires_at<=CURRENT_TIMESTAMP"); }
    private static ResponseStatusException badInput() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"IDs must be unique."); }
}
