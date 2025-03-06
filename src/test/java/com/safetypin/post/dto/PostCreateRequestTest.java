package com.safetypin.post.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PostCreateRequestTest {

    @Test
    public void testGettersAndSetters() {
        // Arrange
        PostCreateRequest request = new PostCreateRequest();
        String title = "Test Title";
        String caption = "Test Caption";
        Double latitude = 12.345;
        Double longitude = 67.890;
        String category = "Test Category";
        
        // Act
        request.setTitle(title);
        request.setCaption(caption);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setCategory(category);
        
        // Assert
        assertEquals(title, request.getTitle());
        assertEquals(caption, request.getCaption());
        assertEquals(latitude, request.getLatitude());
        assertEquals(longitude, request.getLongitude());
        assertEquals(category, request.getCategory());
    }
}
