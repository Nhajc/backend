package com.nhajc.server.artist.service;

import com.nhajc.server.artist.dto.ArtistRequest;
import java.util.UUID;

public interface ArtistService { UUID create(ArtistRequest request); }
