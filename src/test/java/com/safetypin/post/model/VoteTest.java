package com.safetypin.post.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VoteTest {
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private Post post;
    private UUID user1;
    private UUID user2;
    private UUID user3;

    @BeforeEach
    void setUp() {
        post = new Post();
        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
        user3 = UUID.randomUUID();

        Vote vote1 = new Vote(new Vote.VoteId(user1, post), true);  // Upvote
        Vote vote2 = new Vote(new Vote.VoteId(user2, post), false); // Downvote

        post.setVotes(Arrays.asList(vote1, vote2));
    }

    @Test
    void testCurrentVote_Upvote() {
        assertEquals(VoteType.UPVOTE, post.currentVote(user1));
    }

    @Test
    void testCurrentVote_Downvote() {
        assertEquals(VoteType.DOWNVOTE, post.currentVote(user2));
    }

    @Test
    void testCurrentVote_NoVote() {
        assertEquals(VoteType.NONE, post.currentVote(user3));
    }

    @Test
    void testUpvoteCountZero() {

        Post post2 = new Post();
        assertEquals(0, post2.getUpvoteCount());
    }

    @Test
    void testDownvoteCountZero() {
        Post post3 = new Post();
        assertEquals(0, post3.getDownvoteCount());
    }

    @Test
    void UpvoteCountMoreThanZero() {
        assertEquals(1, post.getUpvoteCount());
    }

    @Test
    void DownvoteCountMoreThanZero() {
        assertEquals(1, post.getDownvoteCount());
    }
}
