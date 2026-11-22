package com.nhajc.server.artist.service;
import com.nhajc.server.artist.dto.ArtistResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface ArtistReadService { ArtistResponse get(UUID actor,UUID id,Pageable page); }

