package com.safetypin.post.service;

import com.safetypin.post.model.Location;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
public interface LocationService {

    /**
     * Gets the current user's location if available
     *
     * @return The user's current location as a Point or null if not available
     */
    Point getCurrentUserLocation();

    Location getLocation();

    void setManualLocation(Location location);
}
