package com.safetypin.post.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class DistanceCalculatorTest {

    // Small delta for floating point comparisons
    private static final double DELTA = 0.01;

    @Test
    void testSamePoint() {
        // When the same point is provided, distance should be zero
        double lat = 37.7749;
        double lon = -122.4194;

        double distance = DistanceCalculator.calculateDistance(lat, lon, lat, lon);

        assertEquals(0.0, distance, DELTA);
    }

    @Test
    void testNorthPoleToEquator() {
        // Distance from North Pole to a point on the equator
        double distance = DistanceCalculator.calculateDistance(90.0, 0.0, 0.0, 0.0);

        // Should be approximately 1/4 of Earth's circumference
        assertEquals(6371.0 * Math.PI / 2, distance, 10.0);
    }

    @Test
    void testOppositePoints() {
        // Test antipodal points (opposite sides of Earth)
        double distance = DistanceCalculator.calculateDistance(0.0, 0.0, 0.0, 180.0);

        // Should be approximately half of Earth's circumference
        assertEquals(6371.0 * Math.PI, distance, 10.0);
    }

    @ParameterizedTest
    @CsvSource({
            // Known distances between major cities
            // lat1, lon1, lat2, lon2, expected distance (km)
            "40.7128, -74.0060, 34.0522, -118.2437, 3935.0", // New York to Los Angeles
            "51.5074, -0.1278, 48.8566, 2.3522, 334.0",      // London to Paris
            "35.6762, 139.6503, 22.3193, 114.1694, 2892.0",  // Tokyo to Hong Kong
            "33.9249, 18.4241, 33.8688, 151.2093, 11009.0",  // Cape Town to Sydney
            "-33.4489, -70.6693, -34.6037, -58.3816, 1137.0" // Santiago to Buenos Aires
    })
    void testKnownDistances(double lat1, double lon1, double lat2, double lon2, double expected) {
        double distance = DistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        // Use a larger tolerance for these real-world examples as they might vary
        // slightly based on the exact formula and Earth radius used
        assertEquals(expected, distance, expected * 0.05); // Within 5% of expected value
    }

    @Test
    void testShortDistance() {
        // Test a very short distance (a few hundred meters)
        // Two points in the same neighborhood
        double lat1 = 37.7749;
        double lon1 = -122.4194;
        double lat2 = 37.7750;
        double lon2 = -122.4195;

        double distance = DistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        // Distance should be very small but greater than zero
        assertTrue(distance > 0);
        assertTrue(distance < 0.2); // Less than 200 meters
    }

    @Test
    void testEdgeCases() {
        // Test with extreme latitudes and longitudes

        // Max latitudes
        double distance1 = DistanceCalculator.calculateDistance(90.0, 0.0, -90.0, 0.0);
        assertEquals(6371.0 * Math.PI, distance1, 10.0); // Should be half the Earth's circumference

        // Crossing the date line
        double distance2 = DistanceCalculator.calculateDistance(0.0, 179.9, 0.0, -179.9);
        assertTrue(distance2 < 30.0); // Should be a small distance, not around the world

        // Points at the same longitude but different latitudes
        double distance3 = DistanceCalculator.calculateDistance(0.0, 100.0, 10.0, 100.0);
        assertEquals(6371.0 * Math.toRadians(10.0), distance3, 5.0);
    }

    @Test
    void testHaversineHelper() {
        // Test the private helper method through the interface
        // When dLat = 0 and dLong = 0, the distance should be 0
        double distance = DistanceCalculator.calculateDistance(10.0, 20.0, 10.0, 20.0);
        assertEquals(0.0, distance, DELTA);

        // When only latitude differs, should use the haversine value for latitude difference
        double lat1 = 10.0;
        double lat2 = 11.0;
        double lon = 20.0;

        // Calculate expected based on haversine formula with only latitude difference
        double dLat = Math.toRadians(lat2 - lat1);

        double havLat = Math.pow(Math.sin(dLat / 2), 2);
        double expected = 2 * 6371.0 * Math.asin(Math.sqrt(havLat));

        double actual = DistanceCalculator.calculateDistance(lat1, lon, lat2, lon);
        assertEquals(expected, actual, 0.1);
    }
}