package com.safetypin.post.service;


import com.safetypin.post.model.Location;
import org.springframework.stereotype.Service;

@Service
public class LocationProviderImpl implements LocationProvider {
    @Override
    public boolean isLocationEnabled() {
        // TODO: Implement isLocationEnabled
        return false;
    }

    @Override
    public Location getCurrentLocation() {
        // TODO: Implement getCurrentLocation
        return null;
    }

    @Override
    public Location getLastManualLocation() {
        // TODO: Implement getLastManualLocation
        return null;
    }


    @Override
    public void saveManualLocation(Location location) {
        // TODO Implement saveManualLocation
    }
}
