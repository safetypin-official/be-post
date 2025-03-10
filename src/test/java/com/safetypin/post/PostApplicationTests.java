package com.safetypin.post;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PostApplicationTests {

    @Test
    void testMainDoesNotThrowException() {
        // Calling the main method should load the context without throwing an exception.
        assertDoesNotThrow(() -> PostApplication.main(new String[]{}));
    }
}
