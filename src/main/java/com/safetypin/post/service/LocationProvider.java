package com.safetypin.post.service;

import com.safetypin.post.model.Location;

/**
 * Interface for services that provide location data
 */
public interface LocationProvider {

    /**
     * Check if location services are enabled
     */
    boolean isLocationEnabled();

    /**
     * Get the current device location
     */
    Location getCurrentLocation();

    /**
     * Get the last manually set location
     */
    Location getLastManualLocation();

    /**
     * Save a manually set location
     */
    void saveManualLocation(Location location);
}
