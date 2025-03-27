package com.safetypin.post.controller;

import com.safetypin.post.service.S3Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/post/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    // Endpoint untuk generate pre-signed URL
    @PostMapping("/presigned-url")
    public Map<String, String> getPresignedUrl(@RequestBody Map<String, String> request) {
        String fileType = request.get("fileType"); // e.g., "jpeg", "png"
        URL presignedUrl = s3Service.generatePresignedUrl(fileType);
        return Map.of("url", presignedUrl.toString());
    }
}
