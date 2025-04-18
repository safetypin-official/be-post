package com.safetypin.post.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommentOnCommentTest {

    @Test
    void testSetAndGetParent() {
        CommentOnPost parent = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Test Caption")
                .createdAt(LocalDateTime.now())
                .build();

        CommentOnComment reply = new CommentOnComment();
        reply.setParent(parent);

        assertEquals(parent, reply.getParent());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        CommentOnPost parent = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Parent Caption")
                .createdAt(LocalDateTime.now())
                .build();

        CommentOnComment c1 = CommentOnComment.builder()
                .id(id)
                .caption("Reply")
                .createdAt(LocalDateTime.now())
                .parent(parent)
                .build();

        CommentOnComment c2 = CommentOnComment.builder()
                .id(id)
                .caption("Reply")
                .createdAt(c1.getCreatedAt())
                .parent(parent)
                .build();

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        CommentOnComment comment = new CommentOnComment();
        assertNull(comment.getCreatedAt());

        comment.onCreate();

        assertNotNull(comment.getCreatedAt());
        assertTrue(comment.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(2)));
    }

    @Test
    void testInstanceOfBasePost() {
        CommentOnComment comment = new CommentOnComment();
        assertInstanceOf(BasePost.class, comment);
    }
}
