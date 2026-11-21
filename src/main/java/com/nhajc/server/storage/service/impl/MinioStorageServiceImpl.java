package com.nhajc.server.storage.service.impl;

import com.nhajc.server.storage.exception.ObjectStorageException;
import com.nhajc.server.storage.service.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class MinioStorageServiceImpl implements ObjectStorageService {
    private final MinioClient client;
    private final MinioClient publicClient;
    private final String bucket;
    private final int urlExpirySeconds;

    public MinioStorageServiceImpl(@Qualifier("minioClient") MinioClient client,
                                  @Qualifier("minioPublicClient") MinioClient publicClient,
                                  @Value("${MINIO_BUCKET}") String bucket,
                                  @Value("${MINIO_URL_EXPIRY_SECONDS:3600}") int urlExpirySeconds) {
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
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(input, (long) data.length, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to upload object.", exception);
        }
    }

    @Override
    public byte[] download(String key) {
        try (var input = client.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(key).build())) {
            return input.readAllBytes();
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to download object.", exception);
        }
    }

    @Override
    public String getPresignedUrl(String key) {
        try {
            return publicClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
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
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(key).build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to delete object.", exception);
        }
    }

    private void ensureBucket() throws Exception {
        if (client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            return;
        }
        try {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        } catch (ErrorResponseException exception) {
            if (!"BucketAlreadyOwnedByYou".equals(exception.errorResponse().code())) {
                throw exception;
            }
        }
    }
}
