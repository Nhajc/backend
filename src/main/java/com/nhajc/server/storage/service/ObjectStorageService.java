package com.nhajc.server.storage.service;

public interface ObjectStorageService {
    void upload(String key, byte[] data, String contentType);

    void upload(String key, java.nio.file.Path file, String contentType);

    String getPresignedUrl(String key);

    void delete(String key);
}
