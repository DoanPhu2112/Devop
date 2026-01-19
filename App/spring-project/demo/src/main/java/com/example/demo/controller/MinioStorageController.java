package com.example.demo.controller;

import com.example.demo.dto.UploadResponseDTO;
import com.example.demo.service.MinioStorageService;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@AllArgsConstructor
@RequestMapping(value = "/minio")
public class MinioStorageController {
    @Qualifier("minioStorageServiceImpl")
    private final MinioStorageService storageService;

    @PostMapping(value = "/upload",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/vnd.api.v1+json")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        UploadResponseDTO uploadResponseDTO = storageService.uploadFile(file);
        return ResponseEntity.ok(uploadResponseDTO);
    }

    @GetMapping("/{fileName}")
    public void downloadImage(@PathVariable String fileName, HttpServletResponse response ) throws IOException {
        StatObjectResponse metadata = storageService.getObjectMetadata(fileName);

        String contentType = metadata.contentType();
        long contentLength = metadata.size();

        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        if (contentLength >= 0) {
            response.setContentLengthLong(contentLength);
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (GetObjectResponse objectResponse = storageService.downloadFile(fileName)) {
            objectResponse.transferTo(response.getOutputStream());
        }
    }
}
