package com.nhajc.server.storage.service;

public interface ObjectStorageService {
    void upload(String key, byte[] data, String contentType);

    String getPresignedUrl(String key);

    void delete(String key);
}
