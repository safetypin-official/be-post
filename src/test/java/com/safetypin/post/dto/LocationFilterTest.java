package com.safetypin.post.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class LocationFilterTest {

    @Test
    void testDefaultConstructor() {
        LocationFilter filter = new LocationFilter();
        assertNull(filter.getCategory());
        assertNull(filter.getDateFrom());
        assertNull(filter.getDateTo());
    }

    @Test
    void testParameterizedConstructor() {
        String category = "Traffic";
        LocalDateTime dateFrom = LocalDateTime.now().minusDays(7);
        LocalDateTime dateTo = LocalDateTime.now();

        LocationFilter filter = new LocationFilter(category, dateFrom, dateTo);

        assertEquals(category, filter.getCategory());
        assertEquals(dateFrom, filter.getDateFrom());
        assertEquals(dateTo, filter.getDateTo());
    }

    @Test
    void testGettersAndSetters() {
        // Create using default constructor
        LocationFilter filter = new LocationFilter();

        // Set values
        String category = "Crime";
        LocalDateTime dateFrom = LocalDateTime.now().minusDays(30);
        LocalDateTime dateTo = LocalDateTime.now().minusDays(1);

        filter.setCategory(category);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);

        // Verify values were set correctly
        assertEquals(category, filter.getCategory());
        assertEquals(dateFrom, filter.getDateFrom());
        assertEquals(dateTo, filter.getDateTo());
    }
}
