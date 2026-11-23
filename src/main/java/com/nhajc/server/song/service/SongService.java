package com.nhajc.server.song.service;
import org.springframework.web.multipart.MultipartFile;

import com.nhajc.server.song.dto.UploadSongRequest;
import java.util.UUID;

public interface SongService {
    UUID upload(UUID releaseId, UploadSongRequest request, MultipartFile file);
    String audioUrl(UUID id, String quality);
}
