package com.safetypin.post.model;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PostTest {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void testDefaultConstructor() {
        Post post = new Post();
        assertNull(post.getId());
        assertNull(post.getCaption());
        assertNull(post.getTitle());
        assertNull(post.getCategory());
        assertNull(post.getCreatedAt());
        assertNull(post.getLocation());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();
        Point location = geometryFactory.createPoint(new Coordinate(10.0, 20.0));

        Post post = new Post(id, content, title, category, createdAt, location);

        assertEquals(id, post.getId());
        assertEquals(content, post.getCaption());
        assertEquals(title, post.getTitle());
        assertEquals(category, post.getCategory());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(location, post.getLocation());
    }

    @Test
    void testLatitudeLongitudeConstructor() {
        UUID id = UUID.randomUUID();
        String content = "Test content";
        String title = "Test title";
        String category = "Test category";
        LocalDateTime createdAt = LocalDateTime.now();
        Double latitude = 20.0;
        Double longitude = 10.0;

        Post post = new Post(id, content, title, category, createdAt, latitude, longitude);

        assertEquals(id, post.getId());
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

        assertNull(post.getLatitude());
        assertNull(post.getLongitude());
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
        String category = "Updated category";
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
        post.onCreate();
        
        assertNotNull(post.getId());
        assertNotNull(post.getCreatedAt());
    }
}
