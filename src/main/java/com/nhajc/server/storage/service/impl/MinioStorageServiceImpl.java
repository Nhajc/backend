package com.nhajc.server.storage.service.impl;

import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.storage.service.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MinioStorageServiceImpl implements ObjectStorageService {
    private final MinioClient client;
    private final MinioClient publicClient;
    private final String bucket;
    private final int urlExpirySeconds;

    public MinioStorageServiceImpl(
            @Qualifier("minioClient") MinioClient client,
            @Qualifier("minioPublicClient") MinioClient publicClient,
            @Value("${MINIO_BUCKET}") String bucket,
            @Value("${MINIO_URL_EXPIRY_SECONDS}") int urlExpirySeconds) {
        if (urlExpirySeconds < 1 || urlExpirySeconds > 604800) {
            throw new IllegalArgumentException("MINIO_URL_EXPIRY_SECONDS must be between 1 and 604800.");
        }
        this.client = client;
        this.publicClient = publicClient;
        this.bucket = bucket;
        this.urlExpirySeconds = urlExpirySeconds;
    }

    @Override
    public void upload(String key, byte[] data, String contentType) {
        try (var input = new ByteArrayInputStream(data)) {
            client.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(key).stream(input, (long) data.length, -1L)
                            .contentType(contentType)
                            .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to upload object.", exception);
        }
    }

    @Override
    public void upload(String key, Path file, String contentType) {
        try (var input = Files.newInputStream(file)) {
            client.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(key).stream(input, Files.size(file), -1L)
                            .contentType(contentType)
                            .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to upload object.", exception);
        }
    }

    @Override
    public String getPresignedUrl(String key) {
        try {
            return publicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(urlExpirySeconds)
                            .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to create image URL.", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to delete object.", exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to create the bucket.", exception);
        }
    }
}
