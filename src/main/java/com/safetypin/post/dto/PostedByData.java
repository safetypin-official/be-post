package com.safetypin.post.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PostedByData {
    private UUID userId;
    private String name;
    private String profilePicture;

}