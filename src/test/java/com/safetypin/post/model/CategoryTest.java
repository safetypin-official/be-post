package com.safetypin.post.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CategoryTest {
    @Test
    void testDefaultConstructor() {
        Category category = new Category();
        assertNull(category.getName());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        String name = "Test name";
        Category category = new Category(name);

        assertEquals(name, category.getName());
    }

    @Test
    void testCustomConstructor() {
        String name = "Test name";
        Category category = new Category(name);

        assertEquals(name, category.getName());
    }
}
