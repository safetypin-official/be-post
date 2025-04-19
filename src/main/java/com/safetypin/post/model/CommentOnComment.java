package com.safetypin.post.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@SuperBuilder(builderMethodName = "superBuilder")
@ToString(callSuper = true)
@Table(name = "COMMENT_ON_COMMENT")
public class CommentOnComment extends BasePost {
    @ManyToOne
    @JoinColumn(nullable = false)
    public CommentOnPost parent;

    // Static builder method that sets default values
    public static CommentOnCommentBuilder<?, ?> builder() {
        return superBuilder().createdAt(LocalDateTime.now());
    }

    @PrePersist
    protected void onCreate() {
        if (this.getCreatedAt() == null) {
            this.setCreatedAt(LocalDateTime.now());
        }
    }
}
