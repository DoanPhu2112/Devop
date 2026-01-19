package com.example.demo.service;

import com.example.demo.dto.UploadResponseDTO;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface MinioStorageService {
    public UploadResponseDTO uploadFile(MultipartFile file);

    public GetObjectResponse downloadFile(String fileName);

    public StatObjectResponse getObjectMetadata(String fileName);
}
