package com.safetypin.post.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostExceptionTest {

    @Test
    void testConstructorWithMessageOnly() {
        String message = "Test message";
        PostException exception = new PostException(message);

        assertEquals(message, exception.getMessage());
        assertEquals("POST_ERROR", exception.getErrorCode());
    }

    @Test
    void testConstructorWithMessageAndErrorCode() {
        String message = "Test message";
        String errorCode = "CUSTOM_ERROR";
        PostException exception = new PostException(message, errorCode);

        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Test message";
        Throwable cause = new RuntimeException("Cause");
        PostException exception = new PostException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals("POST_ERROR", exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndErrorCodeAndCause() {
        String message = "Test message";
        String errorCode = "CUSTOM_ERROR";
        Throwable cause = new RuntimeException("Cause");
        PostException exception = new PostException(message, errorCode, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }
}
