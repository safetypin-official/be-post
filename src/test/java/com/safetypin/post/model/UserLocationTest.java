package com.safetypin.post.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserLocationTest {

    @Test
    public void testDefaultConstructor() {
        UserLocation location = new UserLocation();
        assertEquals(0.0, location.getLatitude(), 0.0001);
        assertEquals(0.0, location.getLongitude(), 0.0001);
    }

    @Test
    public void testParameterizedConstructor() {
        double latitude = 37.7749;
        double longitude = -122.4194;
        UserLocation location = new UserLocation(latitude, longitude);

        assertEquals(latitude, location.getLatitude(), 0.0001);
        assertEquals(longitude, location.getLongitude(), 0.0001);
    }

    @Test
    public void testSetLatitude() {
        UserLocation location = new UserLocation();
        double latitude = 40.7128;

        location.setLatitude(latitude);
        assertEquals(latitude, location.getLatitude(), 0.0001);
    }

    @Test
    public void testSetLongitude() {
        UserLocation location = new UserLocation();
        double longitude = -74.0060;

        location.setLongitude(longitude);
        assertEquals(longitude, location.getLongitude(), 0.0001);
    }
}
