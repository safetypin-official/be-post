package com.safetypin.post.dto;

import com.safetypin.post.model.Category;
import lombok.*;

@Data
public class PostCreateRequest {
    private String title;
    private String caption;
    private Double latitude;
    private Double longitude;
    private Category category;
}
