package com.example.demo.config.minio;

import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@AllArgsConstructor
public class MinioConfig {
    private final MinioProperties minio;

    @Bean
    public MinioClient minioClient() {
        if (minio.getEndpoint() == null || minio.getEndpoint().isBlank()) {
            throw new IllegalStateException("Missing required configuration property: minio.endpoint");
        }
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}