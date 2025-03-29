package com.safetypin.post.service;


import com.safetypin.post.model.Post;
import com.safetypin.post.model.Vote;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import com.safetypin.post.exception.InvalidCredentialsException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final JwtService jwtService;

    public String createVote(String authorizationHeader, UUID postId, boolean isUpvote) throws InvalidCredentialsException {
        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);
        Optional<Post> optionalPost = postRepository.findById(postId);

        if (optionalPost.isEmpty()) {
            throw new EntityNotFoundException("Post not found");
        }

        Vote.VoteId voteId = new Vote.VoteId(userId, optionalPost.get());
        Vote vote = new Vote(voteId, isUpvote);

        Optional<Vote> existingVote = voteRepository.findById(voteId);

        if (existingVote.isPresent() && existingVote.get().isUpvote() == isUpvote) {
            if (existingVote.get().isUpvote()) {
                return "User already up voted that post. Vote remains unchanged";
            } else {
                return "User already down voted that post. Vote remains unchanged";
            }
        }

        voteRepository.save(vote);
        return "Vote recorded successfully";
    }

    public String cancelVote(String authorizationHeader, UUID postId) throws InvalidCredentialsException {
        UUID userId = jwtService.getUserIdFromAuthorizationHeader(authorizationHeader);
        Optional<Post> optionalPost = postRepository.findById(postId);

        if (optionalPost.isEmpty()) {
            throw new EntityNotFoundException("Post not found");
        }

        Vote.VoteId voteId = new Vote.VoteId(userId, optionalPost.get());
        Optional<Vote> existingVote = voteRepository.findById(voteId);

        if (existingVote.isPresent()) {
            voteRepository.deleteById(voteId);
            return "Vote cancelled successfully";
        }
        return "User hasn't voted that post. Vote remains unchanged";

    }


}