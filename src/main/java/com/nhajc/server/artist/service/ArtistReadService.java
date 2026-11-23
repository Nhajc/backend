package com.nhajc.server.artist.service;
import com.nhajc.server.artist.dto.ArtistResponse;
import java.util.UUID;
public interface ArtistReadService { ArtistResponse get(UUID actor,UUID id); }

