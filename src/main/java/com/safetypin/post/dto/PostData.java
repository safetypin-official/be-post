package com.safetypin.post.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.safetypin.post.model.Post;
import com.safetypin.post.model.VoteType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostData {

    private UUID id; // Add id field
    private String title;
    private String caption;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private String category;
    private Long upvoteCount;
    private Long downvoteCount;
    private VoteType currentVote;
    private UUID postedById;
    private PostedByData postedBy;

    /**
     * Creates a PostData object from a Post entity
     *
     * @param post   The post entity
     * @param userId The ID of the user viewing the post
     * @return A PostData instance
     */
    public static PostData fromPostAndUserId(Post post, UUID userId, PostedByData postedByData) {

        return PostData.builder()
                .id(post.getId())
                .title(post.getTitle())
                .caption(post.getCaption())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .createdAt(post.getCreatedAt())
                .category(post.getCategory())
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .currentVote(post.currentVote(userId))
                .postedById(post.getPostedBy())
                .postedBy(postedByData)
                .build();
    }
}
