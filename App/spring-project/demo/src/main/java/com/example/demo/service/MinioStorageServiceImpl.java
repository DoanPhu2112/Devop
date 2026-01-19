package com.example.demo.service;

import com.example.demo.config.minio.MinioProperties;
import com.example.demo.dto.UploadResponseDTO;
import io.minio.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@AllArgsConstructor
public class MinioStorageServiceImpl implements MinioStorageService {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public UploadResponseDTO uploadFile(MultipartFile file) {
        try {
            String name = Objects.requireNonNull(file.getOriginalFilename());
            String fileType = file.getContentType();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(name)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(fileType)
                            .build()
            );

            return UploadResponseDTO.builder()
                    .fileName(name)
                    .fileType(fileType)
                    .build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public GetObjectResponse downloadFile(String fileName) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!bucketExists) {
                System.out.println("Bucket does not exist");
            }

            GetObjectResponse objectResponse = minioClient.getObject(
                    GetObjectArgs.builder().bucket(minioProperties.getBucketName()).object(fileName).build()
            );
            return objectResponse;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public StatObjectResponse getObjectMetadata(String fileName) {
        try{
            return minioClient.statObject(
                    StatObjectArgs.builder().bucket(minioProperties.getBucketName()).object(fileName).build()
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
