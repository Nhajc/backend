package com.nhajc.server.release.service.impl;

import com.nhajc.server.release.dto.ReleaseRequest;
import com.nhajc.server.release.entity.Release;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.release.service.ReleaseService;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.genre.entity.Genre;
import com.nhajc.server.genre.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.UUID;

@Service
public class ReleaseServiceImpl implements ReleaseService {
    private final ReleaseRepository releases;
    private final ImageStorage images;
    private final ArtistRepository artists;
    private final GenreRepository genres;
    public ReleaseServiceImpl(ReleaseRepository releases, ImageStorage images, ArtistRepository artists, GenreRepository genres) {
        this.releases = releases; this.images = images; this.artists = artists; this.genres = genres;
    }

    @Override @Transactional
    public UUID create(ReleaseRequest request) {
        if (new HashSet<>(request.artistIds()).size() != request.artistIds().size()
                || new HashSet<>(request.genreIds()).size() != request.genreIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artist and genre IDs must not be repeated.");
        }
        // Consistent lock order prevents opposite input order from deadlocking.
        var releaseArtists = new LinkedHashSet<Artist>();
        var releaseGenres = new LinkedHashSet<Genre>();
        for (UUID id : request.artistIds().stream().sorted().toList()) {
            releaseArtists.add(artists.findLockedById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found.")));
        }
        for (UUID id : request.genreIds().stream().sorted().toList()) {
            releaseGenres.add(genres.findLockedById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Genre not found.")));
        }
        UUID id = UUID.randomUUID();
        String key = images.upload("covers/releases/" + id, images.fromMultipart(request.cover()));
        releases.saveAndFlush(new Release(id, request.title(), request.releaseType(), request.releaseDate(), key,
                releaseArtists, releaseGenres));
        return id;
    }
}
