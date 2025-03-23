package com.safetypin.post.service;

import java.net.URL;

public interface S3Service {
    /**
     * Generates a pre-signed URL for uploading a file to S3
     * 
     * @param fileType the file extension (e.g., "jpg", "png")
     * @return a pre-signed URL that can be used to upload the file
     */
    URL generatePresignedUrl(String fileType);
}