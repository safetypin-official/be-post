package com.safetypin.post.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.safetypin.post.model.Location;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationProvider locationProvider;
    
    private LocationServiceImpl locationService;
    
    @BeforeEach
    void setUp() {
        locationService = new LocationServiceImpl(locationProvider);
    }

    @Test
    void retriveCurrentLocationWhenEnabled() {
        // Setup
        Location expectedLocation = new Location(0.0, 0.0);
        when(locationProvider.isLocationEnabled()).thenReturn(true);
        when(locationProvider.getCurrentLocation()).thenReturn(expectedLocation);
        
        // Execute
        Location actualLocation = locationService.getLocation();
        
        // Assert
        assertEquals(expectedLocation, actualLocation, "Should return current location when enabled");
        verify(locationProvider).getCurrentLocation();
    }
    
    @Test
    void useLastManualLocationWhenDisabled() {
        // Setup
        Location manualLocation = new Location(1.0, 1.0);
        when(locationProvider.isLocationEnabled()).thenReturn(false);
        when(locationProvider.getLastManualLocation()).thenReturn(manualLocation);
        
        // Execute
        Location actualLocation = locationService.getLocation();
        
        // Assert
        assertEquals(manualLocation, actualLocation, "Should return last manual location when disabled");
        verify(locationProvider).getLastManualLocation();
    }
    
    @Test
    void promptForLocationWhenNoPriorLocationExists() {
        // Setup
        when(locationProvider.isLocationEnabled()).thenReturn(false);
        when(locationProvider.getLastManualLocation()).thenReturn(null);
        
        // Execute
        Location actualLocation = locationService.getLocation();
        
        // Assert
        assertNull(actualLocation, "Should return null when no location is available");
    }
    
    @Test
    void setAndRetrieveManualLocation() {
        // Setup
        Location manualLocation = new Location(2.0, 2.0);
        
        // Execute
        locationService.setManualLocation(manualLocation);
        
        // Assert
        verify(locationProvider).saveManualLocation(manualLocation);
        
        // Setup for retrieval test
        when(locationProvider.isLocationEnabled()).thenReturn(false);
        when(locationProvider.getLastManualLocation()).thenReturn(manualLocation);
        
        // Execute retrieval
        Location retrievedLocation = locationService.getLocation();
        
        // Assert
        assertEquals(manualLocation, retrievedLocation, "Should retrieve the manually set location");
    }
}
