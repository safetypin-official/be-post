package com.safetypin.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Getter
@Setter
@AllArgsConstructor
public class CommentDTOWithPostId {
    CommentDTO comment;
    UUID postId;
}
