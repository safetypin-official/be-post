package com.safetypin.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplTest {

    @InjectMocks
    private S3ServiceImpl s3Service;

    @Mock
    private S3Presigner presigner;

    @Mock
    private S3Presigner.Builder presignerBuilder;

    @Mock
    private DefaultCredentialsProvider credentialsProvider;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private final String testBucketName = "test-bucket";
    private final String testRegion = "us-east-1";
    private final String testFileType = "jpeg";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", testBucketName);
        ReflectionTestUtils.setField(s3Service, "region", testRegion);
    }

    @Test
    void generatePresignedUrl_Success() throws Exception {
        // Arrange
        URL mockUrl = new URI("https://test-bucket.s3.amazonaws.com/test-file.jpeg").toURL();

        try (MockedStatic<S3Presigner> mockedS3Presigner = mockStatic(S3Presigner.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentialsProvider = mockStatic(DefaultCredentialsProvider.class)) {

            // Mock static methods
            mockedS3Presigner.when(S3Presigner::builder).thenReturn(presignerBuilder);
            mockedCredentialsProvider.when(DefaultCredentialsProvider::create).thenReturn(credentialsProvider);

            // Mock builder chain
            when(presignerBuilder.region(Region.of(testRegion))).thenReturn(presignerBuilder);
            when(presignerBuilder.credentialsProvider(credentialsProvider)).thenReturn(presignerBuilder);
            when(presignerBuilder.build()).thenReturn(presigner);

            // Use lenient mocking for the consumer parameter
            lenient().when(presigner.presignPutObject(any(Consumer.class))).thenReturn(presignedPutObjectRequest);
            when(presignedPutObjectRequest.url()).thenReturn(mockUrl);

            // Act
            URL result = s3Service.generatePresignedUrl(testFileType);

            // Assert
            assertNotNull(result);
            assertEquals(mockUrl, result);

            // Verify the presigner was called and closed
            verify(presigner).close();
        }
    }

    @Test
    void generatePresignedUrl_ThrowsException() {
        // Arrange
        Exception expectedException = new RuntimeException("Test exception");

        try (MockedStatic<S3Presigner> mockedS3Presigner = mockStatic(S3Presigner.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentialsProvider = mockStatic(DefaultCredentialsProvider.class)) {

            // Mock static methods
            mockedS3Presigner.when(S3Presigner::builder).thenReturn(presignerBuilder);
            mockedCredentialsProvider.when(DefaultCredentialsProvider::create).thenReturn(credentialsProvider);

            // Mock builder chain
            when(presignerBuilder.region(Region.of(testRegion))).thenReturn(presignerBuilder);
            when(presignerBuilder.credentialsProvider(credentialsProvider)).thenReturn(presignerBuilder);
            when(presignerBuilder.build()).thenReturn(presigner);

            // Mock throwing an exception
            lenient().when(presigner.presignPutObject(any(Consumer.class))).thenThrow(expectedException);

            // Act & Assert
            Exception exception = assertThrows(RuntimeException.class, () -> s3Service.generatePresignedUrl(testFileType));
            assertEquals(expectedException, exception);

            // Verify the presigner was closed
            verify(presigner).close();
        }
    }

    @Test
    void testFileNameGeneration() throws Exception {
        // Create a test to verify the UUID-based filename generation
        try (MockedStatic<S3Presigner> mockedS3Presigner = mockStatic(S3Presigner.class);
             MockedStatic<DefaultCredentialsProvider> mockedCredentialsProvider = mockStatic(DefaultCredentialsProvider.class)) {

            // Mock static methods
            mockedS3Presigner.when(S3Presigner::builder).thenReturn(presignerBuilder);
            mockedCredentialsProvider.when(DefaultCredentialsProvider::create).thenReturn(credentialsProvider);

            // Mock builder chain
            when(presignerBuilder.region(Region.of(testRegion))).thenReturn(presignerBuilder);
            when(presignerBuilder.credentialsProvider(credentialsProvider)).thenReturn(presignerBuilder);
            when(presignerBuilder.build()).thenReturn(presigner);

            // Create a mock URL for the response
            URL mockUrl = URI.create("https://test-bucket.s3.amazonaws.com/test-file.jpeg").toURL();

            // Use argument capture with a custom answer to inspect the PutObjectRequest
            lenient().when(presigner.presignPutObject(any(Consumer.class))).thenAnswer(invocation -> {
                Consumer<PutObjectPresignRequest.Builder> consumer = invocation.getArgument(0);

                // Create a mock builder to pass to the consumer
                PutObjectPresignRequest.Builder builder = mock(PutObjectPresignRequest.Builder.class);

                // Capture the PutObjectRequest during the builder chain
                final PutObjectRequest[] capturedRequest = new PutObjectRequest[1];
                when(builder.signatureDuration(any(Duration.class))).thenReturn(builder);
                when(builder.putObjectRequest(any(PutObjectRequest.class))).thenAnswer(innerInvocation -> {
                    capturedRequest[0] = innerInvocation.getArgument(0);
                    return builder;
                });

                // Execute consumer with our mocked builder
                consumer.accept(builder);

                // Verify captured request
                assertNotNull(capturedRequest[0], "PutObjectRequest was not captured");

                // Verify the key (filename) format
                String key = capturedRequest[0].key();
                assertNotNull(key, "Key should not be null");
                assertTrue(key.endsWith("." + testFileType), "Key should end with the file extension");

                // Verify bucket and content type
                assertEquals(testBucketName, capturedRequest[0].bucket(), "Bucket name should match");
                assertEquals("image/" + testFileType, capturedRequest[0].contentType(), "Content type should be set correctly");

                // Verify filename follows UUID pattern
                String filenamePattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\." + testFileType;
                assertTrue(key.matches(filenamePattern), "Filename should match UUID pattern with extension");

                // Return mock response
                when(presignedPutObjectRequest.url()).thenReturn(mockUrl);
                return presignedPutObjectRequest;
            });

            // Act
            URL result = s3Service.generatePresignedUrl(testFileType);

            // Assert URL was returned correctly
            assertNotNull(result);
            assertEquals(mockUrl, result);

            // Verify the presigner was closed
            verify(presigner).close();
        }
    }
}