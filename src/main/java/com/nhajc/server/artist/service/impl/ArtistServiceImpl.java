package com.nhajc.server.artist.service.impl;

import com.nhajc.server.artist.dto.ArtistRequest;
import com.nhajc.server.artist.entity.Artist;
import com.nhajc.server.artist.repository.ArtistRepository;
import com.nhajc.server.artist.service.ArtistService;
import com.nhajc.server.storage.ImageStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ArtistServiceImpl implements ArtistService {
    private final ArtistRepository artists;
    private final ImageStorage images;
    public ArtistServiceImpl(ArtistRepository artists, ImageStorage images) { this.artists = artists; this.images = images; }

    @Override @Transactional
    public UUID create(ArtistRequest request) {
        UUID id = UUID.randomUUID();
        String key = images.upload("avatars/artists/" + id, images.fromMultipart(request.avatar()));
        artists.saveAndFlush(new Artist(id, request.name(), key));
        return id;
    }
}
