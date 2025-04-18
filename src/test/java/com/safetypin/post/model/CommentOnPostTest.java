package com.safetypin.post.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommentOnPostTest {

    @Test
    void testSetAndGetParent() {
        Post parentPost = Post.builder()
                .id(UUID.randomUUID())
                .title("Sample Title")
                .caption("Sample Caption")
                .category("General")
                .createdAt(LocalDateTime.now())
                .location(0.0, 0.0)
                .postedBy(UUID.randomUUID())
                .build();

        CommentOnPost comment = new CommentOnPost();
        comment.setParent(parentPost);

        assertEquals(parentPost, comment.getParent());
    }

    @Test
    void testSetAndGetComments() {
        CommentOnComment comment1 = new CommentOnComment();
        CommentOnComment comment2 = new CommentOnComment();

        List<CommentOnComment> replies = List.of(comment1, comment2);

        CommentOnPost commentOnPost = new CommentOnPost();
        commentOnPost.setComments(replies);

        assertEquals(replies, commentOnPost.getComments());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        Post parent = new Post();
        parent.setId(UUID.randomUUID());

        CommentOnPost comment1 = CommentOnPost.builder()
                .id(id)
                .caption("Test caption")
                .createdAt(LocalDateTime.now())
                .parent(parent)
                .build();

        CommentOnPost comment2 = CommentOnPost.builder()
                .id(id)
                .caption("Test caption")
                .createdAt(comment1.getCreatedAt())
                .parent(parent)
                .build();

        assertEquals(comment1, comment2);
        assertEquals(comment1.hashCode(), comment2.hashCode());
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        CommentOnPost comment = new CommentOnPost();
        assertNull(comment.getCreatedAt());

        comment.onCreate();
        assertNotNull(comment.getCreatedAt());
        assertTrue(comment.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(2)));
    }

    @Test
    void testInstanceOfBasePost() {
        CommentOnPost comment = new CommentOnPost();
        assertInstanceOf(BasePost.class, comment);
    }
}
