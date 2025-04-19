package com.safetypin.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    // used for commenting a post/comment
    @NotBlank
    @Size(max = 1000)
    private String caption;

    // Optional: if replying to another comment
    private UUID parentId;

    // userId is not included because it can be taken from SecurityContextHolder
}