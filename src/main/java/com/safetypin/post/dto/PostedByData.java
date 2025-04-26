package com.safetypin.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class PostedByData {
    private UUID userId;
    private String name;
    private String profilePicture;

}