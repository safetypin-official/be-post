package com.safetypin.post.dto;

import com.safetypin.post.model.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostCreateRequestTest {

    @Test
    void testGettersAndSetters() {
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
