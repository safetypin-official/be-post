package com.safetypin.post.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "COMMENT_ON_POST")
public class CommentOnPost extends BasePost {
    @ManyToOne
    @JoinColumn(nullable = false)
    public Post parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private transient List<CommentOnComment> comments;


    @PrePersist
    protected void onCreate() {
        this.setCreatedAt(LocalDateTime.now());
    }
}
