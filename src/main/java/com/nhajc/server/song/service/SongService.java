package com.nhajc.server.song.service;

import com.nhajc.server.song.dto.UploadSongRequest;
import java.util.UUID;

public interface SongService {
    UUID upload(UUID releaseId, UploadSongRequest request);
    String audioUrl(UUID id, String quality);
}
