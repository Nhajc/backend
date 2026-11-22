package com.nhajc.server.playlist.service;
import com.nhajc.server.playlist.dto.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface PlaylistService {
    PlaylistResponse get(UUID actor, UUID id, Pageable page);
    PlaylistResponse create(UUID actor, PlaylistRequest request);
    void update(UUID actor, UUID id, PlaylistRequest request);
    void delete(UUID actor, UUID id);
    void addSong(UUID actor, UUID id, UUID song);
    void removeSong(UUID actor, UUID id, UUID song);
    void reorder(UUID actor, UUID id, List<UUID> songIds);
}

