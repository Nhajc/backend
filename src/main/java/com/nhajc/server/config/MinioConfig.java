package com.nhajc.server.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import okhttp3.OkHttpClient;

import java.time.Duration;

@Configuration
@EnableScheduling
public class MinioConfig {

    @Bean
    @Primary
    public MinioClient minioClient(
            @Value("${S3_ENDPOINT}") String endpoint,
            @Value("${MINIO_ROOT_USER}") String accessKey,
            @Value("${MINIO_ROOT_PASSWORD}") String secretKey,
            @Value("${S3_REGION}") String region
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .httpClient(new OkHttpClient.Builder()
                        .callTimeout(Duration.ofMinutes(2))
                        .build())
                .build();
    }

    @Bean("minioPublicClient")
    public MinioClient minioPublicClient(
            @Value("${S3_PUBLIC_ENDPOINT}") String endpoint,
            @Value("${MINIO_ROOT_USER}") String accessKey,
            @Value("${MINIO_ROOT_PASSWORD}") String secretKey,
            @Value("${S3_REGION}") String region
    ) {
        // The signed host must match the address used by the frontend.
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}
