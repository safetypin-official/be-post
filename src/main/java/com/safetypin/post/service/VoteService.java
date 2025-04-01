package com.safetypin.post.service;

import java.util.UUID;

public interface VoteService {

    // Cast a vote (upvote or downvote) for a post
    String createVote(UUID userId, UUID postId, boolean isUpvote);

    // Cancel an existing vote
    String cancelVote(UUID userId, UUID postId);
}