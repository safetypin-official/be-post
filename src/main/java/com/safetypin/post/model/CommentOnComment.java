package com.safetypin.post.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@SuperBuilder(builderMethodName = "superBuilder")
@ToString(callSuper = true)
@Table(name = "COMMENT_ON_COMMENT")
@Getter
@Setter
public class CommentOnComment extends BasePost {
    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonIgnore
    public CommentOnPost parent;

    // Static builder method that sets default values
    public static CommentOnCommentBuilder<?, ?> builder() {
        return superBuilder().createdAt(LocalDateTime.now());
    }

    @PrePersist
    protected void onCreate() {
        this.setCreatedAt(LocalDateTime.now());
    }
}
