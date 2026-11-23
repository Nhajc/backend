package com.nhajc.server.release.service.impl;
import org.springframework.web.multipart.MultipartFile;

import com.nhajc.server.release.dto.ReleaseRequest;
import com.nhajc.server.release.entity.Release;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.release.service.ReleaseService;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.storage.ObjectStorageCleanup;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.genre.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.UUID;

@Service
public class ReleaseServiceImpl implements ReleaseService {
    private final ReleaseRepository releases;
    private final ImageStorage images;
    private final ObjectStorageCleanup cleanup;
    private final ArtistRepository artists;
    private final GenreRepository genres;
    private final TransactionTemplate transaction;

    public ReleaseServiceImpl(ReleaseRepository releases, ImageStorage images, ArtistRepository artists,
                              GenreRepository genres, ObjectStorageCleanup cleanup,
                              PlatformTransactionManager manager) {
        this.releases = releases;
        this.images = images;
        this.artists = artists;
        this.genres = genres;
        this.cleanup = cleanup;
        this.transaction = new TransactionTemplate(manager);
    }

    @Override
    public UUID create(ReleaseRequest request, MultipartFile file) {
        if (new HashSet<>(request.artistIds()).size() != request.artistIds().size()
                || new HashSet<>(request.genreIds()).size() != request.genreIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artist and genre IDs must not be repeated.");
        }
        UUID id = UUID.randomUUID();
        String key = images.upload("covers/releases/" + id + "/cover", images.fromMultipart(file));
        transaction.executeWithoutResult(status -> {
            var releaseArtists = new LinkedHashSet<>(artists.findAllById(request.artistIds()));
            var releaseGenres = new LinkedHashSet<>(genres.findAllById(request.genreIds()));
            if (releaseArtists.size() != request.artistIds().size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found.");
            }
            if (releaseGenres.size() != request.genreIds().size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre not found.");
            }
            releases.saveAndFlush(new Release(id, request.title(), request.releaseType(),
                    request.releaseDate(), key, releaseArtists, releaseGenres));
            cleanup.completeUpload(key);
        });
        return id;
    }
}
