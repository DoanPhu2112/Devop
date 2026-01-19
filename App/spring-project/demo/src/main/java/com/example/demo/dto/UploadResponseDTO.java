package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UploadResponseDTO {
    String fileName;
    String fileType;
}
