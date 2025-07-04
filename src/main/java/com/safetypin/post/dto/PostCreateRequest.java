package com.safetypin.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PostCreateRequest {
    @JsonProperty(value = "Title", required = true)
    private String title;

    @JsonProperty(value = "Caption", required = true)
    private String caption;

    @JsonProperty(value = "Latitude", required = true)
    private Double latitude;

    @JsonProperty(value = "Longitude", required = true)
    private Double longitude;

    @JsonProperty(value = "Category", required = true)
    private String category;

    private UUID postedBy; // The HTTP request doesn't have to fill this field (it will be replaced with userId from jwtToken anyway)
    private String imageUrl;

    @JsonProperty(value = "Address", required = true)
    private String address;
}
