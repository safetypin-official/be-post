package com.safetypin.post.model;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostTest {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void testDefaultConstructor() {
        Post post = new Post();
        assertNull(post.getId());
        assertNull(post.getCaption());
        assertNotNull(post.getTitle());
        assertNull(post.getCategory());
        assertNull(post.getCreatedAt());
        assertNotNull(post.getLocation());
    }

    @Test
    void testAllArgsConstructor() {
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();
        Point location = geometryFactory.createPoint(new Coordinate(10.0, 20.0));

        Post post = new Post(content, title, category, createdAt, 20.0, 10.0);

        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(location, post.getLocation());
    }

    @Test
    void testLatitudeLongitudeConstructor() {
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();
        Double latitude = 20.0;
        Double longitude = 10.0;

        Post post = new Post(content, title, category, createdAt, latitude, longitude);

        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
    }

    @Test
    void testGetLatitudeLongitude() {
        Post post = new Post();
        Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
        post.setLocation(point);

        assertEquals(20.0, post.getLatitude(), 0.0001);
        assertEquals(10.0, post.getLongitude(), 0.0001);
    }

    @Test
    void testGetLatitudeLongitudeWithNullLocation() {
        Post post = new Post();

        assertNotNull(post.getLatitude());
        assertNotNull(post.getLongitude());
    }

    @Test
    void testSetLatitude() {
        Post post = new Post();
        // First set longitude to have a point to update
        post.setLongitude(10.0);
        post.setLatitude(20.0);

        assertEquals(20.0, post.getLatitude(), 0.0001);
        assertEquals(10.0, post.getLongitude(), 0.0001);
    }

    @Test
    void testSetLatitudeWithNullLocation() {
        Post post = new Post();
        post.setLatitude(20.0);

        assertEquals(20.0, post.getLatitude(), 0.0001);
        assertEquals(0.0, post.getLongitude(), 0.0001);
    }

    @Test
    void testSetLongitude() {
        Post post = new Post();
        // First set latitude to have a point to update
        post.setLatitude(20.0);
        post.setLongitude(10.0);

        assertEquals(20.0, post.getLatitude(), 0.0001);
        assertEquals(10.0, post.getLongitude(), 0.0001);
    }

    @Test
    void testSetLongitudeWithNullLocation() {
        Post post = new Post();
        post.setLongitude(10.0);

        assertEquals(0.0, post.getLatitude(), 0.0001);
        assertEquals(10.0, post.getLongitude(), 0.0001);
    }

    @Test
    void testSetters() {
        Post post = new Post();

        UUID id = UUID.randomUUID();
        String content = "Updated content";
        String title = "Updated title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();
        Point location = geometryFactory.createPoint(new Coordinate(15.0, 25.0));

        post.setId(id);
        post.setCaption(content);
        post.setTitle(title);
        post.setCategory(category);
        post.setCreatedAt(createdAt);
        post.setLocation(location);

        assertEquals(id, post.getId());
        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(location, post.getLocation());
    }

    @Test
    void testPrePersistGeneratesUuid() {
        Post post = new Post();
        UUID id = UUID.randomUUID();
        post.setId(id);
        post.onCreate();
        assertEquals(id, post.getId());
        assertNotNull(post.getCreatedAt());
    }

    @Test
    void testConstructorThrowsExceptionWhenLatitudeIsNull() {
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Post(content, title, category, createdAt, null, 10.0);
        });

        assertEquals("Latitude cannot be null", exception.getMessage());
    }

    @Test
    void testConstructorThrowsExceptionWhenLongitudeIsNull() {
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Post(content, title, category, createdAt, 20.0, null);
        });

        assertEquals("Longitude cannot be null", exception.getMessage());
    }

    @Test
    void testConstructorThrowsExceptionWhenBothCoordinatesAreNull() {
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Post(content, title, category, createdAt, null, null);
        });

        assertEquals("Both latitude and longitude cannot be null", exception.getMessage());
    }

    @Test
    void testBuilderThrowsExceptionWhenCoordinatesAreNotProvided() {
        Post.Builder builder = Post.builder()
                .caption("Test content")
                .title("Test title")
                .category("Test category");

        Exception exception = assertThrows(IllegalArgumentException.class,
                builder::build);

        assertEquals("Both latitude and longitude must be provided", exception.getMessage());
    }

    @Test
    void testBuilderThrowsExceptionWhenOnlyLatitudeIsProvided() {
        Post.Builder builder = Post.builder()
                .caption("Test content")
                .title("Test title")
                .category("Test category")
                .latitude(20.0);

        Exception exception = assertThrows(IllegalArgumentException.class,
                builder::build);

        assertEquals("Both latitude and longitude must be provided", exception.getMessage());
    }

    @Test
    void testBuilderThrowsExceptionWhenOnlyLongitudeIsProvided() {
        Post.Builder builder = Post.builder()
                .caption("Test content")
                .title("Test title")
                .category("Test category")
                .longitude(10.0);

        Exception exception = assertThrows(IllegalArgumentException.class,
                builder::build);

        assertEquals("Both latitude and longitude must be provided", exception.getMessage());
    }

    @Test
    void testBuilderCreatesValidPost() {
        UUID id = UUID.randomUUID();
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();
        Double latitude = 20.0;
        Double longitude = 10.0;

        Post post = Post.builder()
                .id(id)
                .caption(content)
                .title(title)
                .category(category)
                .createdAt(createdAt)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        assertEquals(id, post.getId());
        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
    }

    @Test
    void testBuilderWithLocationMethod() {
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        Double latitude = 20.0;
        Double longitude = 10.0;

        Post post = Post.builder()
                .caption(content)
                .title(title)
                .category(category)
                .location(latitude, longitude)  // Using the location method
                .build();

        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
        assertNotNull(post.getCreatedAt());  // Should auto-generate
    }

    @Test
    void testBuilderWithRequiredFieldsOnly() {
        // Latitude and longitude are required fields and must be provided
        String title = "Test title";
        String category = "Test category";
        Double latitude = 20.0;
        Double longitude = 10.0;

        Post post = Post.builder()
                .title(title)
                .category(category)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
        assertNotNull(post.getCreatedAt());  // Should auto-generate
        assertNull(post.getCaption());       // Optional field
    }

    @Test
    void testBuilderWithNoCreatedAt() {
        // Even without createdAt, latitude and longitude must still be provided
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        Double latitude = 20.0;
        Double longitude = 10.0;

        LocalDateTime beforeBuild = LocalDateTime.now();
        Post post = Post.builder()
                .caption(content)
                .title(title)
                .category(category)
                .latitude(latitude)
                .longitude(longitude)
                .build();
        LocalDateTime afterBuild = LocalDateTime.now();

        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(latitude, post.getLatitude());
        assertEquals(longitude, post.getLongitude());
        assertNotNull(post.getCreatedAt());

        // Verify that createdAt was auto-generated within the expected timeframe
        assertTrue(
                !post.getCreatedAt().isBefore(beforeBuild) &&
                        !post.getCreatedAt().isAfter(afterBuild),
                "CreatedAt should be auto-generated between beforeBuild and afterBuild"
        );
    }

    @Test
    void testSetLatitudeWithNullValue() {
        Post post = new Post();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            post.setLatitude(null);
        });

        assertEquals("Latitude cannot be null", exception.getMessage());
    }

    @Test
    void testSetLongitudeWithNullValue() {
        Post post = new Post();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            post.setLongitude(null);
        });

        assertEquals("Longitude cannot be null", exception.getMessage());
    }

    @Test
    void testSetLatitudeWithUninitializedLocation() {
        Post post = new Post();
        // Use reflection to set location to null
        try {
            java.lang.reflect.Field locationField = Post.class.getDeclaredField("location");
            locationField.setAccessible(true);
            locationField.set(post, null);
        } catch (Exception e) {
            fail("Failed to set location to null: " + e.getMessage());
        }

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            post.setLatitude(20.0);
        });

        assertEquals("Location must be initialized before setting latitude", exception.getMessage());
    }

    @Test
    void testSetLongitudeWithUninitializedLocation() {
        Post post = new Post();
        // Use reflection to set location to null
        try {
            java.lang.reflect.Field locationField = Post.class.getDeclaredField("location");
            locationField.setAccessible(true);
            locationField.set(post, null);
        } catch (Exception e) {
            fail("Failed to set location to null: " + e.getMessage());
        }

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            post.setLongitude(10.0);
        });

        assertEquals("Location must be initialized before setting longitude", exception.getMessage());
    }
}
