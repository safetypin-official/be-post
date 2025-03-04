package com.safetypin.post.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationTest {

    @Test
    void testDefaultConstructor() {
        Location location = new Location();
        assertEquals(0.0, location.getLatitude(), 0.0001);
        assertEquals(0.0, location.getLongitude(), 0.0001);
    }

    @Test
    void testParameterizedConstructor() {
        double latitude = 37.7749;
        double longitude = -122.4194;
        Location location = new Location(latitude, longitude);

        assertEquals(latitude, location.getLatitude(), 0.0001);
        assertEquals(longitude, location.getLongitude(), 0.0001);
    }

    @Test
    void testSetters() {
        Location location = new Location();
        double latitude = 40.7128;
        double longitude = -74.0060;

        location.setLatitude(latitude);
        location.setLongitude(longitude);

        assertEquals(latitude, location.getLatitude(), 0.0001);
        assertEquals(longitude, location.getLongitude(), 0.0001);
    }

    @Test
    void testEqualsAndHashCode() {
        Location location1 = new Location(37.7749, -122.4194);
        Location location2 = new Location(37.7749, -122.4194);
        Location location3 = new Location(40.7128, -74.0060);

        // Test equals
        assertEquals(location1, location2);
        assertNotEquals(location1, location3);

        // Test hashCode
        assertEquals(location1.hashCode(), location2.hashCode());
        assertNotEquals(location1.hashCode(), location3.hashCode());
    }

    @Test
    void testToString() {
        Location location = new Location(37.7749, -122.4194);
        String toString = location.toString();

        assertTrue(toString.contains("latitude=37.7749"));
        assertTrue(toString.contains("longitude=-122.4194"));
    }
}
