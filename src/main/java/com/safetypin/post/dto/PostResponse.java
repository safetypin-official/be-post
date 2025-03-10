package com.safetypin.post.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
public class PostResponse {
    private boolean success;
    private String message;
    private Object data;
}