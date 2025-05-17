package com.safetypin.post.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.safetypin.post.dto.PostResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void testHandleInvalidPostData() {
        // Given
        String errorMessage = "Invalid post data";
        InvalidPostDataException exception = new InvalidPostDataException(errorMessage);

        // When
        ResponseEntity<PostResponse> response = exceptionHandler.handleInvalidPostData(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandlePostException() {
        // Given
        String errorMessage = "Post error occurred";
        PostException exception = new PostException(errorMessage);

        // When
        ResponseEntity<PostResponse> response = exceptionHandler.handlePostException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandleArgumentTypeMismatch() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("postId");

        // When
        ResponseEntity<PostResponse> response = exceptionHandler.handleArgumentTypeMismatch(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid parameter format: postId", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandleGenericException() {
        // Given
        String errorMessage = "Something went wrong";
        Exception exception = new RuntimeException(errorMessage);

        // When
        ResponseEntity<PostResponse> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred: " + errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
