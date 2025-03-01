package com.safetypin.post.config;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GeometryConfiguration.class)
class GeometryConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testGeometryFactoryBean() {
        // Check that the bean exists
        assertTrue(applicationContext.containsBean("geometryFactory"));
        
        // Get the bean
        GeometryFactory factory = applicationContext.getBean(GeometryFactory.class);
        
        // Verify it's not null
        assertNotNull(factory);
        
        // Verify SRID is set to 4326 (WGS84)
        assertEquals(4326, factory.getSRID());
        
        // Verify it's using the correct precision model
        PrecisionModel expectedPrecisionModel = new PrecisionModel();
        assertEquals(expectedPrecisionModel.getType(), factory.getPrecisionModel().getType());
        assertEquals(expectedPrecisionModel.getScale(), factory.getPrecisionModel().getScale());
    }
}
