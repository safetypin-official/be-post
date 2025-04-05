package com.safetypin.post.controller;

import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.dto.UserDetails;
import com.safetypin.post.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController

@RequestMapping("/vote")
public class VoteController {

    final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/upvote")
    public ResponseEntity<PostResponse> upvote(@RequestParam UUID postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

        try {
            return createSuccessResponse(voteService.createVote(userId, postId, true));
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @PostMapping("/downvote")
    public ResponseEntity<PostResponse> downvote(@RequestParam UUID postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

        try {
            return createSuccessResponse(voteService.createVote(userId, postId, false));
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @DeleteMapping("/cancel-vote")
    public ResponseEntity<PostResponse> cancelVote(@RequestParam UUID postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

        try {
            return createSuccessResponse(voteService.cancelVote(userId, postId));
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }

    }


    // Helper method to create success responses
    private ResponseEntity<PostResponse> createSuccessResponse(String message) {
        PostResponse response = new PostResponse(true, message, null);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private ResponseEntity<PostResponse> createErrorResponse(String message) {
        PostResponse errorResponse = new PostResponse(false, message, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
