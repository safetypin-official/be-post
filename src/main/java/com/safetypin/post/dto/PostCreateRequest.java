package com.safetypin.post.dto;

import lombok.Data;

@Data
public class PostCreateRequest {
    private String title;
    private String caption;
    private Double latitude;
    private Double longitude;
    private String category;
}
