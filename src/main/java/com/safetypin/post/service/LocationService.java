package com.safetypin.post.service;

import com.safetypin.post.model.Location;
import org.locationtech.jts.geom.Point;

public interface LocationService {
    
    /**
     * Gets the current user's location if available
     * @return Point representing the user's location or null if not available
     */
    Point getCurrentUserLocation();
    
    /**
     * Gets the user's location from various sources
     * @return Location object or null if no location is available
     */
    Location getLocation();
    
    /**
     * Sets a manual location for the user
     * @param location The location to set
     */
    void setManualLocation(Location location);
}
