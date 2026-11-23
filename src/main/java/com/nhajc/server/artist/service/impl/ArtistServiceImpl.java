package com.nhajc.server.artist.service.impl;
import org.springframework.web.multipart.MultipartFile;

import com.nhajc.server.artist.dto.ArtistRequest;
import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.storage.ImageStorage;
import com.nhajc.server.storage.ObjectStorageCleanup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;

@Service
public class ArtistServiceImpl implements ArtistService {
    private final ArtistRepository artists;
    private final ImageStorage images;
    private final ObjectStorageCleanup cleanup;
    private final TransactionTemplate transaction;

    public ArtistServiceImpl(ArtistRepository artists, ImageStorage images,
                             ObjectStorageCleanup cleanup, PlatformTransactionManager manager) {
        this.artists = artists;
        this.images = images;
        this.cleanup = cleanup;
        this.transaction = new TransactionTemplate(manager);
    }

    @Override
    public UUID create(ArtistRequest request, MultipartFile file) {
        UUID id = UUID.randomUUID();
        String key = images.upload("avatars/artists/" + id + "/avatar",
                images.fromMultipart(file));
        transaction.executeWithoutResult(status -> {
            artists.saveAndFlush(new Artist(id, request.name(), key));
            cleanup.completeUpload(key);
        });
        return id;
    }
}
