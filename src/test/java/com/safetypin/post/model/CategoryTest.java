package com.safetypin.post.model;

import org.junit.jupiter.api.Test;

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

        String name = "Test name";
        Category category = new Category(name);

        assertEquals(name, category.getName());
    }
}
