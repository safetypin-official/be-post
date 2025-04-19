package com.safetypin.post.dto;

import java.time.LocalDateTime;
import java.util.List;
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
    private PostedByData postedBy;

    /**
     * Creates a PostData object from a Post entity
     *
     * @param post   The post entity
     * @param userId The ID of the user viewing the post
     * @return A PostData instance
     */
    public static PostData fromPostAndUserId(Post post, UUID userId, List<PostedByData> profiles) {

        PostedByData postedByData = new PostedByData(post.getPostedBy(), null, null);
        if (profiles == null || profiles.isEmpty()) {
            // can't fetch postedBy
        } else {
            // iterate profiles
            PostedByData profileResponse = null;
            for (PostedByData profile : profiles) {
                if (profile.getId().equals(post.getPostedBy())) {
                    profileResponse = profile;
                    break;
                }
            }
            if (profileResponse == null) {
                // can't fetch postedBy
            } else {
                postedByData = PostedByData.builder()
                        .id(userId)
                        .profilePicture(profileResponse.getProfilePicture())
                        .name(profileResponse.getName())
                        .build();
            }

        }

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
                .postedBy(postedByData)
                .build();
    }
}
