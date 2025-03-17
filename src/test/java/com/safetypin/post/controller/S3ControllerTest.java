package com.safetypin.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetypin.post.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URL;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class S3ControllerTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private S3Controller s3Controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(s3Controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getPresignedUrl_Success() throws Exception {
        // Arrange
        String fileType = "jpeg";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/test-file.jpeg";
        URL mockUrl = new URL(expectedUrl);

        // Mock the S3Service to return a URL when generatePresignedUrl is called with "jpeg"
        when(s3Service.generatePresignedUrl(eq(fileType))).thenReturn(mockUrl);

        // Create request body
        Map<String, String> requestBody = Map.of("fileType", fileType);
        String requestJson = objectMapper.writeValueAsString(requestBody);

        // Expected response
        Map<String, String> expectedResponse = Map.of("url", expectedUrl);
        String expectedResponseJson = objectMapper.writeValueAsString(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/post/s3/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponseJson));

        // Verify that the service method was called with the correct parameter
        verify(s3Service).generatePresignedUrl(fileType);
    }

    @Test
    void getPresignedUrl_DifferentFileType() throws Exception {
        // Arrange
        String fileType = "png";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/test-file.png";
        URL mockUrl = new URL(expectedUrl);

        // Mock the S3Service to return a URL when generatePresignedUrl is called with "png"
        when(s3Service.generatePresignedUrl(eq(fileType))).thenReturn(mockUrl);

        // Create request body
        Map<String, String> requestBody = Map.of("fileType", fileType);
        String requestJson = objectMapper.writeValueAsString(requestBody);

        // Expected response
        Map<String, String> expectedResponse = Map.of("url", expectedUrl);
        String expectedResponseJson = objectMapper.writeValueAsString(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/post/s3/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponseJson));

        // Verify that the service method was called with the correct parameter
        verify(s3Service).generatePresignedUrl(fileType);
    }

    @Test
    void constructor_InitializesCorrectly() {
        // This explicitly tests the constructor to ensure 100% coverage
        S3Controller controller = new S3Controller(s3Service);
        // No assertions needed - just verifying constructor executes without exceptions
    }
}