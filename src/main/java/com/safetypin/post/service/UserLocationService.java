package com.safetypin.post.service;

import com.safetypin.post.model.UserLocation;

/**
 * Service for managing user location information
 */
public interface UserLocationService {
    
    /**
     * Retrieves the current user's location
     * @return the current user's location
     */
    UserLocation getCurrentUserLocation();
    
    /**
     * Updates the current user's location
     * @param latitude the user's latitude
     * @param longitude the user's longitude
     * @return the updated user location
     */
    UserLocation updateUserLocation(double latitude, double longitude);
}
