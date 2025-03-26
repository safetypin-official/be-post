package com.safetypin.post.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Vote {

    @EmbeddedId
    private VoteId id;
    @Column(nullable = false)
    private boolean isUpvote; // true for upvote, false for downvote

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteId implements Serializable {
        private UUID userId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        private Post post;
    }
}
