package com.safetypin.post.model;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostBuilderTest {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void testBuilderWithAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        String title = "Test Post";
        String caption = "This is a test post";
        String category = "Safety";
        LocalDateTime createdAt = LocalDateTime.now();
        Double latitude = 40.7128;
        Double longitude = -74.0060;

        // When
        Post post = Post.builder()
                .id(id)
                .title(title)
                .caption(caption)
                .category(category)
                .createdAt(createdAt)
                .location(latitude, longitude)
                .build();

        // Then
        assertEquals(id, post.getId());
        assertEquals(title, post.getTitle());
        assertEquals(caption, post.getCaption());
        assertEquals(category, post.getCategory());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
    }

    @Test
    void testBuilderWithRequiredFieldsOnly() {
        // Given
        String caption = "This is a test post";

        // When
        Post post = Post.builder()
                .caption(caption)
                .build();

        // Then
        assertNull(post.getId());
        assertNull(post.getTitle());
        assertEquals(caption, post.getCaption());
        assertNull(post.getCategory());
        assertNotNull(post.getCreatedAt()); // Should be auto-generated
        assertNull(post.getLatitude());
        assertNull(post.getLongitude());
    }

    @Test
    void testBuilderWithSeparateLocationSetters() {
        // Given
        Double latitude = 37.7749;
        Double longitude = -122.4194;

        // When
        Post post = Post.builder()
                .caption("Required caption")
                .latitude(latitude)
                .longitude(longitude)
                .build();

        // Then
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
    }

    @Test
    void testBuilderWithNoCreatedAt() {
        // When
        Post post = Post.builder()
                .caption("Required caption")
                .build();

        // Then
        assertNotNull(post.getCreatedAt());
        assertTrue(post.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(post.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }
}
