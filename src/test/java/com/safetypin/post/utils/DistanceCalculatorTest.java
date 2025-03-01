package com.safetypin.post.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DistanceCalculatorTest {

    private static final double TOLERANCE = 0.01; // Tolerance for floating point comparisons

    @Test
    public void testDistanceBetweenSameLocation() {
        // Setup: Calculate distance from (0,0) to (0,0)
        double distance = DistanceCalculator.calculateDistance(0.0, 0.0, 0.0, 0.0);
        
        // Assertion: Distance equals 0 km
        assertEquals(0.0, distance, TOLERANCE);
    }

    @Test
    public void testDistanceBetweenKnownPoints() {
        // Setup: Calculate from (0,0) to (1,0) (~111.32 km)
        double distance = DistanceCalculator.calculateDistance(0.0, 0.0, 1.0, 0.0);
        
        // Assertion: Distance is approximately 111.32 km (within tolerance)
        assertEquals(111.32, distance, TOLERANCE);
    }

    @Test
    public void testDistanceAcrossLargeDistance() {
        // Setup: Calculate from (0,0) to (90,0)
        double distance = DistanceCalculator.calculateDistance(0.0, 0.0, 90.0, 0.0);
        
        // Assertion: Distance matches expected value (~10,007 km)
        assertEquals(10007.0, distance, TOLERANCE);
    }
}
