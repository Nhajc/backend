package com.nhajc.server.artist.service;

import com.nhajc.server.artist.dto.ArtistRequest;
import com.nhajc.server.artist.dto.ArtistResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ArtistService {
    UUID create(ArtistRequest request, MultipartFile file);

    ArtistResponse get(UUID actor, UUID id);
}
