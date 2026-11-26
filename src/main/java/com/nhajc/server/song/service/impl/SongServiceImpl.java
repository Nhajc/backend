package com.nhajc.server.song.service.impl;

import com.nhajc.server.common.exception.DatabaseConflicts;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.song.audio.*;
import com.nhajc.server.song.dto.UploadSongRequest;
import com.nhajc.server.song.entity.Song;
import com.nhajc.server.song.repository.SongRepository;
import com.nhajc.server.song.service.SongService;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.storage.ObjectStorageCleanup;
import com.nhajc.server.storage.service.ObjectStorageService;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SongServiceImpl implements SongService {
    private static final Logger log = LoggerFactory.getLogger(SongServiceImpl.class);
    private static final long MAX_BYTES = 200L * 1024 * 1024;
    private final Semaphore slots = new Semaphore(2);
    private final SongRepository songs;
    private final ReleaseRepository releases;
    private final AudioEncoder encoder;
    private final ImageStorage images;
    private final ObjectStorageService storage;
    private final ObjectStorageCleanup cleanup;
    private final TransactionTemplate transaction;

    public SongServiceImpl(
            SongRepository songs,
            ReleaseRepository releases,
            AudioEncoder encoder,
            ImageStorage images,
            ObjectStorageService storage,
            ObjectStorageCleanup cleanup,
            PlatformTransactionManager transactionManager) {
        this.songs = songs;
        this.releases = releases;
        this.encoder = encoder;
        this.images = images;
        this.storage = storage;
        this.cleanup = cleanup;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public UUID upload(UUID releaseId, UploadSongRequest request, MultipartFile file) {
        Integer trackNumber = request.trackNumber();
        if (trackNumber == null || trackNumber < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A positive track number is required.");
        if (songs.existsByRelease_IdAndTrackNumber(releaseId, trackNumber)) throw duplicateTrack();
        return prepare(
                releaseId,
                file,
                prepared -> {
                    transaction.executeWithoutResult(status -> {
                        var release =
                                releases.findByIdForUpdate(releaseId)
                                        .orElseThrow(() ->
                                                new ResponseStatusException(
                                                        HttpStatus.NOT_FOUND, "Release not found."));
                        if (songs.existsByRelease_IdAndTrackNumber(releaseId, trackNumber)) throw duplicateTrack();
                        songs.saveAndFlush(
                                new Song(
                                        prepared.id(),
                                        request.title(),
                                        prepared.durationSeconds(),
                                        trackNumber,
                                        release,
                                        prepared.keys()));
                        if (prepared.coverKey() != null
                                && releases.setCoverIfAbsent(releaseId, prepared.coverKey()) == 1)
                            cleanup.completeUpload(prepared.coverKey());
                        prepared.keys().values().forEach(cleanup::completeUpload);
                    });
                    return prepared.id();
                });
    }

    public record PreparedSong(UUID id, int durationSeconds, Map<String, String> keys, String coverKey) {}

    /** Shared processing capacity for both direct publication and staged uploads. */
    public UUID prepare(UUID releaseId, MultipartFile file, java.util.function.Function<PreparedSong, UUID> persist) {
        if (!slots.tryAcquire())
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "At most two song uploads can be processed at once.");
        Path directory = null;
        try {
            if (!releases.existsById(releaseId))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Release not found.");
            if (file.isEmpty()) throw FlacMetadata.invalid();
            if (file.getSize() > MAX_BYTES) throw tooLarge();
            directory = Files.createTempDirectory("nhajc-audio-");
            Path original = directory.resolve("original.flac");
            try (var input = file.getInputStream();
                    var output = Files.newOutputStream(original)) {
                byte[] buffer = new byte[65536];
                long copied = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    copied += read;
                    if (copied > MAX_BYTES) throw tooLarge();
                    output.write(buffer, 0, read);
                }
            }
            FlacMetadata metadata = FlacMetadata.read(original);
            Map<String, Path> files = encoder.encode(original, directory, metadata);
            if (!files.keySet().equals(Set.of("low", "medium", "high", "lossless")))
                throw new IllegalStateException("Encoder did not produce all qualities.");
            ImageStorage.Image cover = null;
            if (metadata.picture() != null) {
                try {
                    cover = images.normalize(metadata.picture(), metadata.pictureMime());
                } catch (ResponseStatusException exception) {
                    log.info("Ignoring unsupported embedded cover");
                }
            }
            UUID songId = UUID.randomUUID();
            Map<String, String> keys = new LinkedHashMap<>();
            for (var entry : files.entrySet()) {
                String quality = entry.getKey();
                String key = "audio/songs/" + songId + "/" + quality + (quality.equals("lossless") ? ".flac" : ".m4a");
                // Independent reservations survive failed uploads, DB rollbacks and crashes.
                cleanup.reserveUpload(key, Duration.ofHours(2));
                storage.upload(key, entry.getValue(), quality.equals("lossless") ? "audio/flac" : "audio/mp4");
                keys.put(quality, key);
            }
            String coverKey = images.upload("covers/releases/" + releaseId + "/cover", cover, Duration.ofHours(2));
            return persist.apply(new PreparedSong(songId, metadata.durationSeconds(), Map.copyOf(keys), coverKey));
        } catch (DataIntegrityViolationException exception) {
            throw DatabaseConflicts.translate(exception, "Track number already exists in this release.");
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Unable to read or process audio upload.", exception);
        } finally {
            if (directory != null) {
                try (var paths = Files.walk(directory)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (IOException exception) {
                    log.warn("Unable to remove audio temporary directory {}", directory);
                }
            }
            slots.release();
        }
    }

    @Override
    public String audioUrl(UUID id, String quality) {
        if (!Set.of("low", "medium", "high", "lossless").contains(quality)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audio quality.");
        }
        return storage.getPresignedUrl(
                songs.findAudioKey(id, quality)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song audio not found.")));
    }

    private static ResponseStatusException duplicateTrack() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Track number already exists in this release.");
    }

    private static ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Audio file must be at most 200 MB.");
    }
}
