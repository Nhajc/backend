package com.nhajc.server.artist.service.impl;

import com.nhajc.server.artist.dto.ArtistRequest;
import com.nhajc.server.artist.dto.ArtistResponse;
import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.common.service.*;
import com.nhajc.server.release.repository.ReleaseRepository;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.storage.ObjectStorageCleanup;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArtistServiceImpl implements ArtistService {
    private final ArtistRepository artists;
    private final ImageStorage images;
    private final ObjectStorageCleanup cleanup;
    private final TransactionTemplate transaction;
    private final ReleaseRepository releases;
    private final UserAccess users;
    private final MediaViews views;

    public ArtistServiceImpl(
            ArtistRepository artists,
            ImageStorage images,
            ObjectStorageCleanup cleanup,
            PlatformTransactionManager manager,
            ReleaseRepository releases,
            UserAccess users,
            MediaViews views) {
        this.artists = artists;
        this.images = images;
        this.cleanup = cleanup;
        this.transaction = new TransactionTemplate(manager);
        this.releases = releases;
        this.users = users;
        this.views = views;
    }

    @Override
    public UUID create(ArtistRequest request, MultipartFile file) {
        UUID id = UUID.randomUUID();
        String key = images.upload("avatars/artists/" + id + "/avatar", images.fromMultipart(file));
        transaction.executeWithoutResult(status -> {
            artists.saveAndFlush(new Artist(id, request.name(), key));
            cleanup.completeUpload(key);
        });
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistResponse get(UUID actor, UUID id) {
        users.require(actor);
        var artist =
                artists.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found."));
        var values = releases.findForArtist(id);
        var mapped = views.releases(values.stream().map(release -> release.getId()).toList(), actor);
        return new ArtistResponse(
                views.artist(artist, actor), values.stream().map(release -> mapped.get(release.getId())).toList());
    }
}
