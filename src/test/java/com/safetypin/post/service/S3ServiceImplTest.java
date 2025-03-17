package com.safetypin.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplTest {

    @Mock
    private DefaultCredentialsProvider credentialsProvider;

    @InjectMocks
    private S3ServiceImpl s3Service;

    private final String testBucketName = "test-bucket";
    private final String testRegion = "us-east-1";
    private final String testFileType = "jpeg";

    @BeforeEach
    void setUp() {
        // Set up the test environment with lenient mock settings
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(s3Service, "bucketName", testBucketName);
        ReflectionTestUtils.setField(s3Service, "region", testRegion);
    }

    @Test
    void generatePresignedUrl_Success() throws Exception {
        // Arrange
        URL mockUrl = new URI("https://test-bucket.s3.amazonaws.com/test-file.jpeg").toURL();

        // Use Mockito.mockStatic with LENIENT settings
        MockSettings lenientSettings = Mockito.withSettings().lenient();

        try (MockedStatic<S3Presigner> s3PresignerMockedStatic = Mockito.mockStatic(S3Presigner.class, lenientSettings);
             MockedStatic<DefaultCredentialsProvider> defaultCredentialsProviderMockedStatic =
                     Mockito.mockStatic(DefaultCredentialsProvider.class, lenientSettings)) {

            // Create mocks with lenient settings
            S3Presigner.Builder mockBuilder = mock(S3Presigner.Builder.class, lenientSettings);
            S3Presigner mockPresigner = mock(S3Presigner.class, lenientSettings);
            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class, lenientSettings);
            DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class, lenientSettings);

            // Setup static mocks
            s3PresignerMockedStatic.when(S3Presigner::builder).thenReturn(mockBuilder);
            defaultCredentialsProviderMockedStatic.when(DefaultCredentialsProvider::create).thenReturn(mockCredentialsProvider);

            // Setup builder chain
            lenient().when(mockBuilder.region(any(Region.class))).thenReturn(mockBuilder);
            lenient().when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
            lenient().when(mockBuilder.build()).thenReturn(mockPresigner);

            // Use doAnswer with lenient settings
            doAnswer(invocation -> {
                // Just return the mocked presigned request without trying to execute the consumer
                return mockPresignedRequest;
            }).when(mockPresigner).presignPutObject(any(Consumer.class));

            // Setup URL return
            lenient().when(mockPresignedRequest.url()).thenReturn(mockUrl);

            // Act
            URL result = s3Service.generatePresignedUrl(testFileType);

            // Assert
            assertNotNull(result);
            assertEquals(mockUrl, result);
        }
    }

    @Test
    void generatePresignedUrl_ThrowsException() {
        // Arrange
        Exception expectedException = new RuntimeException("Test exception");

        // Use Mockito.mockStatic with LENIENT settings
        MockSettings lenientSettings = Mockito.withSettings().lenient();

        try (MockedStatic<S3Presigner> s3PresignerMockedStatic = Mockito.mockStatic(S3Presigner.class, lenientSettings);
             MockedStatic<DefaultCredentialsProvider> defaultCredentialsProviderMockedStatic =
                     Mockito.mockStatic(DefaultCredentialsProvider.class, lenientSettings)) {

            // Create mocks with lenient settings
            S3Presigner.Builder mockBuilder = mock(S3Presigner.Builder.class, lenientSettings);
            S3Presigner mockPresigner = mock(S3Presigner.class, lenientSettings);
            DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class, lenientSettings);

            // Setup static mocks
            s3PresignerMockedStatic.when(S3Presigner::builder).thenReturn(mockBuilder);
            defaultCredentialsProviderMockedStatic.when(DefaultCredentialsProvider::create).thenReturn(mockCredentialsProvider);

            // Setup builder chain
            lenient().when(mockBuilder.region(any(Region.class))).thenReturn(mockBuilder);
            lenient().when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
            lenient().when(mockBuilder.build()).thenReturn(mockPresigner);

            // Setup exception throwing using doThrow
            doThrow(expectedException).when(mockPresigner).presignPutObject(any(Consumer.class));

            // Act & Assert
            Exception exception = assertThrows(RuntimeException.class, () -> s3Service.generatePresignedUrl(testFileType));
            assertEquals(expectedException, exception);
        }
    }

    @Test
    void testFileNameGeneration() throws Exception {
        // Create a class to capture the filename
        class FilenameCapturer {
            String key;
            String bucket;
            String contentType;
        }

        // Create a capturer instance
        FilenameCapturer capturer = new FilenameCapturer();

        // Arrange
        URL mockUrl = new URI("https://test-bucket.s3.amazonaws.com/test-file.jpeg").toURL();

        // Use Mockito.mockStatic with LENIENT settings
        MockSettings lenientSettings = Mockito.withSettings().lenient();

        try (MockedStatic<S3Presigner> s3PresignerMockedStatic = Mockito.mockStatic(S3Presigner.class, lenientSettings);
             MockedStatic<DefaultCredentialsProvider> defaultCredentialsProviderMockedStatic =
                     Mockito.mockStatic(DefaultCredentialsProvider.class, lenientSettings)) {

            // Create mocks with lenient settings
            S3Presigner.Builder mockBuilder = mock(S3Presigner.Builder.class, lenientSettings);
            S3Presigner mockPresigner = mock(S3Presigner.class, lenientSettings);
            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class, lenientSettings);
            DefaultCredentialsProvider mockCredentialsProvider = mock(DefaultCredentialsProvider.class, lenientSettings);

            // Create a mock PutObjectPresignRequest.Builder that will work with our consumer
            PutObjectPresignRequest.Builder presignRequestBuilder = new PutObjectPresignRequest.Builder() {
                @Override
                public PutObjectPresignRequest.Builder signatureDuration(Duration duration) {
                    return this;
                }

                @Override
                public PutObjectPresignRequest.Builder putObjectRequest(PutObjectRequest putObjectRequest) {
                    // Capture the key, bucket, and content type
                    capturer.key = putObjectRequest.key();
                    capturer.bucket = putObjectRequest.bucket();
                    capturer.contentType = putObjectRequest.contentType();
                    return this;
                }

                @Override
                public PutObjectPresignRequest build() {
                    return mock(PutObjectPresignRequest.class);
                }
            };

            // Setup static mocks
            s3PresignerMockedStatic.when(S3Presigner::builder).thenReturn(mockBuilder);
            defaultCredentialsProviderMockedStatic.when(DefaultCredentialsProvider::create).thenReturn(mockCredentialsProvider);

            // Setup builder chain
            lenient().when(mockBuilder.region(any(Region.class))).thenReturn(mockBuilder);
            lenient().when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
            lenient().when(mockBuilder.build()).thenReturn(mockPresigner);

            // Setup consumer with custom implementation
            doAnswer(invocation -> {
                Consumer<PutObjectPresignRequest.Builder> consumer = invocation.getArgument(0);

                // This is the key part - execute the consumer with our custom builder implementation
                consumer.accept(presignRequestBuilder);

                return mockPresignedRequest;
            }).when(mockPresigner).presignPutObject(any(Consumer.class));

            // Setup URL return
            lenient().when(mockPresignedRequest.url()).thenReturn(mockUrl);

            // Act
            URL result = s3Service.generatePresignedUrl(testFileType);

            // Assert URL was returned correctly
            assertNotNull(result);
            assertEquals(mockUrl, result);

            // Verify captured values from the request
            assertNotNull(capturer.key, "Key should not be null");
            assertEquals(testBucketName, capturer.bucket, "Bucket name should match");
            assertEquals("image/" + testFileType, capturer.contentType, "Content type should be set correctly");

            // Verify filename format (UUID pattern plus file extension)
            String key = capturer.key;
            assertTrue(key.endsWith("." + testFileType), "Key should end with the file extension");

            String uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\." + testFileType;
            assertTrue(Pattern.matches(uuidPattern, key), "Filename should match UUID pattern with extension");
        }
    }
}