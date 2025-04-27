package com.safetypin.post.dto;

import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
public class CommentDTO {
    private final LocalDateTime createdAt;
    private final UUID postedById;
    private final PostedByData postedBy;
    UUID id;
    String caption;

    public CommentDTO(CommentOnPost commentOnPost, PostedByData postedBy) {
        this.id = commentOnPost.getId();
        this.caption = commentOnPost.getCaption();
        this.createdAt = commentOnPost.getCreatedAt();
        this.postedById = commentOnPost.getPostedBy();
        this.postedBy = postedBy;
    }

    public CommentDTO(CommentOnComment commentOnComment, PostedByData postedBy) {
        this.id = commentOnComment.getId();
        this.caption = commentOnComment.getCaption();
        this.createdAt = commentOnComment.getCreatedAt();
        this.postedById = commentOnComment.getPostedBy();
        this.postedBy = postedBy;
    }
}
