package com.safetypin.post.service;

import com.safetypin.post.model.Location;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationProvider locationProvider;
    private final GeometryFactory geometryFactory;

    public LocationServiceImpl(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
        this.geometryFactory = new GeometryFactory();
    }

    @Override
    public Point getCurrentUserLocation() {
        Location location = getLocation();
        if (location == null) {
            return null;
        }
        return geometryFactory.createPoint(new Coordinate(location.getLongitude(), location.getLatitude()));
    }

    @Override
    public Location getLocation() {
        if (locationProvider.isLocationEnabled()) {
            return locationProvider.getCurrentLocation();
        } else {
            return locationProvider.getLastManualLocation();
        }
    }

    @Override
    public void setManualLocation(Location location) {
        locationProvider.saveManualLocation(location);
    }
}
