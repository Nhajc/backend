package com.nhajc.server.artist.service;
import org.springframework.web.multipart.MultipartFile;

import com.nhajc.server.artist.dto.ArtistRequest;
import java.util.UUID;

public interface ArtistService { UUID create(ArtistRequest request, MultipartFile file); }
