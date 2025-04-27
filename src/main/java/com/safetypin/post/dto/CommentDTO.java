package com.safetypin.post.dto;

import com.safetypin.post.model.BasePost;
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

    public CommentDTO(BasePost comment, PostedByData postedBy) {
        this.id = comment.getId();
        this.caption = comment.getCaption();
        this.createdAt = comment.getCreatedAt();
        this.postedById = comment.getPostedBy();
        this.postedBy = postedBy;
    }
}
