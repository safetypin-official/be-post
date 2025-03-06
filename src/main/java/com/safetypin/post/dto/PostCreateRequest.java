package com.safetypin.post.dto;

import lombok.Getter;
import lombok.Setter;

public class PostCreateRequest {
    // Getters and setters
    @Setter
    @Getter
    private String title;
    @Setter
    @Getter
    private String caption;
    @Setter
    @Getter
    private Double latitude;
    @Setter
    @Getter
    private Double longitude;
    @Setter
    @Getter
    private String category;

}
