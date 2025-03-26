package com.safetypin.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

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

    private UUID postedBy;

    public UUID getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(UUID postedBy) {
        this.postedBy = postedBy;
    }
}
