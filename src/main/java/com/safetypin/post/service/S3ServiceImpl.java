package com.safetypin.post.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    @Value("${aws.s3.bucket-name:default}")
    private String bucketName;

    @Value("${aws.s3.region:default}")
    private String region;

    @Override
    public URL generatePresignedUrl(String fileType) {
        log.info("Generating presigned URL for file type: {}", fileType);
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            String fileName = UUID.randomUUID() + "." + fileType;
            log.debug("Generated unique filename: {}", fileName);

            PresignedPutObjectRequest preSignedRequest = presigner.presignPutObject(builder ->
                    builder.signatureDuration(Duration.ofMinutes(10))
                            .putObjectRequest(req -> req
                                    .bucket(bucketName)
                                    .key(fileName)
                                    .contentType("image/" + fileType)
                            )
            );

            URL presignedUrl = preSignedRequest.url();
            log.info("Successfully generated presigned URL for bucket: {}, key: {}", bucketName, fileName);
            log.debug("Presigned URL: {}", presignedUrl);

            return presignedUrl;
        } catch (Exception e) {
            log.error("Error generating presigned URL for file type: {}", fileType, e);
            throw e;
        }
    }
}