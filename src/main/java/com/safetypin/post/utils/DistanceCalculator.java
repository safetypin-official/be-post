package com.safetypin.post.utils;

/**
 * Utility class for calculating geographical distances between coordinates
 */
public class DistanceCalculator {

    // Earth radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the distance between two points on Earth using the Haversine formula.
     *
     * @param lat1 Latitude of the first point in degrees
     * @param lon1 Longitude of the first point in degrees
     * @param lat2 Latitude of the second point in degrees
     * @param lon2 Longitude of the second point in degrees
     * @return Distance in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double dLat = Math.toRadians((lat2 - lat1));
        double dLong = Math.toRadians((lon2 - lon1));

        // Convert latitudes to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double a = calculateHaversineValue(dLat) + Math.cos(lat1) * Math.cos(lat2) * calculateHaversineValue(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
        }

        private static double calculateHaversineValue(double angle) {
        return Math.pow(Math.sin(angle / 2), 2);
    }
}
