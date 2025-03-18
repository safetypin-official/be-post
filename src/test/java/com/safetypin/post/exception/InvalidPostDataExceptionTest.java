package com.safetypin.post.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidPostDataExceptionTest {

    @Test
    void testConstructorWithMessage() {
        // Given
        String errorMessage = "Test error message";

        // When
        InvalidPostDataException exception = new InvalidPostDataException(errorMessage);

        // Then
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void testExceptionInheritance() {
        // When
        InvalidPostDataException exception = new InvalidPostDataException("Test message");

        // Then
        assertInstanceOf(RuntimeException.class, exception);
    }
}
