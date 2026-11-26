package com.nhajc.server.song.service;

import com.nhajc.server.song.dto.UploadSongRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface SongService {
    UUID upload(UUID releaseId, UploadSongRequest request, MultipartFile file);

    String audioUrl(UUID id, String quality);
}
