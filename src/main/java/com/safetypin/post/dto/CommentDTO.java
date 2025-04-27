package com.safetypin.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.safetypin.post.model.BasePost;
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
    private final UUID id;
    private final String caption;
    private final LocalDateTime createdAt;
    private final UUID postedById;
    private final PostedByData postedBy;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Long commentCount;

    public CommentDTO(BasePost comment, PostedByData postedBy) {
        this.id = comment.getId();
        this.caption = comment.getCaption();
        this.createdAt = comment.getCreatedAt();
        this.postedById = comment.getPostedBy();
        this.postedBy = postedBy;
        if (comment instanceof CommentOnPost) {
            this.commentCount = ((CommentOnPost) comment).getChildCount();
        } else {
            this.commentCount = null;
        }
    }
}
