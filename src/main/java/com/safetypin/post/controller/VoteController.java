package com.safetypin.post.controller;

import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController

@RequestMapping("/vote")
public class VoteController {

    VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/upvote")
    public ResponseEntity<PostResponse> upvote(@RequestHeader("Authorization") String authorizationHeader, @RequestParam UUID postId) {
        try {
            return createSuccessResponse(voteService.createVote(authorizationHeader, postId, true));
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/downvote")
    public ResponseEntity<PostResponse> downvote(@RequestHeader("Authorization") String authorizationHeader, @RequestParam UUID postId) {
        try {
            return createSuccessResponse(voteService.createVote(authorizationHeader, postId, false));
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/cancel-vote")
    public ResponseEntity<PostResponse> cancelVote(@RequestHeader("Authorization") String authorizationHeader, @RequestParam UUID postId) {
        try {
            return createSuccessResponse(voteService.cancelVote(authorizationHeader, postId));
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }


    // Helper method to create success responses
    private ResponseEntity<PostResponse> createSuccessResponse(String message) {
        PostResponse response = new PostResponse(true, message, null);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private ResponseEntity<PostResponse> createErrorResponse(HttpStatus status, String message) {
        PostResponse errorResponse = new PostResponse(false, message, null);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
