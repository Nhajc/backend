package com.nhajc.server.playlist.service;
import org.springframework.web.multipart.MultipartFile;
import com.nhajc.server.playlist.dto.*;
import java.util.*;
public interface PlaylistService {
    PlaylistResponse get(UUID actor, UUID id);
    PlaylistResponse create(UUID actor, PlaylistRequest request, MultipartFile file);
    void update(UUID actor, UUID id, PlaylistRequest request, MultipartFile file);
    void delete(UUID actor, UUID id);
    void addSong(UUID actor, UUID id, UUID song);
    void removeSong(UUID actor, UUID id, UUID song);
    void reorder(UUID actor, UUID id, List<UUID> songIds);
}

