package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidCredentialsException;

import java.util.UUID;

public interface VoteService {

    // Cast a vote (upvote or downvote) for a post
    String createVote(String authorizationHeader, UUID postId, boolean isUpvote) throws InvalidCredentialsException;

    // Cancel an existing vote
    String cancelVote(String authorizationHeader, UUID postId) throws InvalidCredentialsException;
}