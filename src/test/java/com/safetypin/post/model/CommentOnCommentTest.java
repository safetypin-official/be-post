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
    void testPrePersistBehavior() {
        // Verify that the @PrePersist annotation works correctly
        // by checking that the method conditionally sets createdAt
        CommentOnComment comment = new CommentOnComment();
        assertNull(comment.getCreatedAt());

        // Direct invocation of the method that has the @PrePersist annotation
        comment.onCreate();

        // The method should set a timestamp
        LocalDateTime createdAt = comment.getCreatedAt();
        assertNotNull(createdAt);
        assertTrue(createdAt.isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(createdAt.isAfter(LocalDateTime.now().minusSeconds(10)));

        // Test that the method preserves existing timestamps
        LocalDateTime existingTimestamp = comment.getCreatedAt();

        // Wait a small amount of time to ensure timestamps would be different if reset
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            fail("Test was interrupted");
        }

        comment.onCreate();
        LocalDateTime newTimestamp = comment.getCreatedAt();

        // The timestamps should be the same because CommentOnComment only sets
        // createdAt if it was null
        assertEquals(existingTimestamp, newTimestamp);
    }

    @Test
    void testInstanceOfBasePost() {
        CommentOnComment comment = new CommentOnComment();
        assertInstanceOf(BasePost.class, comment);
    }

    @Test
    void testDefaultConstructor() {
        CommentOnComment comment = new CommentOnComment();

        assertNull(comment.getId());
        assertNull(comment.getCaption());
        assertNull(comment.getCreatedAt());
        assertNull(comment.getParent());
        assertNull(comment.getPostedBy());
    }

    @Test
    void testSuperBuilderWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID postedBy = UUID.randomUUID();
        String caption = "Test Reply Caption";
        LocalDateTime createdAt = LocalDateTime.now();
        CommentOnPost parent = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Parent Caption")
                .createdAt(LocalDateTime.now())
                .build();

        CommentOnComment comment = CommentOnComment.builder()
                .id(id)
                .caption(caption)
                .createdAt(createdAt)
                .postedBy(postedBy)
                .parent(parent)
                .build();

        assertEquals(id, comment.getId());
        assertEquals(caption, comment.getCaption());
        assertEquals(createdAt, comment.getCreatedAt());
        assertEquals(postedBy, comment.getPostedBy());
        assertEquals(parent, comment.getParent());
    }

    @Test
    void testSuperBuilderWithRequiredFieldsOnly() {
        String caption = "Required caption";
        CommentOnPost parent = CommentOnPost.builder()
                .caption("Parent Caption")
                .build();

        CommentOnComment comment = CommentOnComment.builder()
                .caption(caption)
                .parent(parent)
                .build();

        assertNull(comment.getId());
        assertEquals(caption, comment.getCaption());
        assertNotNull(comment.getCreatedAt()); // Should be auto-generated
        assertNull(comment.getPostedBy());
        assertEquals(parent, comment.getParent());
    }

    @Test
    void testSetAndGetCaption() {
        CommentOnComment comment = new CommentOnComment();
        String caption = "Test caption for reply";

        comment.setCaption(caption);

        assertEquals(caption, comment.getCaption());
    }

    @Test
    void testSetAndGetPostedBy() {
        CommentOnComment comment = new CommentOnComment();
        UUID postedBy = UUID.randomUUID();

        comment.setPostedBy(postedBy);

        assertEquals(postedBy, comment.getPostedBy());
    }

    @Test
    void testSetAndGetId() {
        CommentOnComment comment = new CommentOnComment();
        UUID id = UUID.randomUUID();

        comment.setId(id);

        assertEquals(id, comment.getId());
    }

    @Test
    void testSetAndGetCreatedAt() {
        CommentOnComment comment = new CommentOnComment();
        LocalDateTime createdAt = LocalDateTime.now();

        comment.setCreatedAt(createdAt);

        assertEquals(createdAt, comment.getCreatedAt());
    }

    @Test
    void testEqualsWithDifferentObjects() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CommentOnPost parent1 = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Parent Caption 1")
                .build();
        CommentOnPost parent2 = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Parent Caption 2")
                .build();

        CommentOnComment c1 = CommentOnComment.builder()
                .id(id1)
                .caption("Reply 1")
                .parent(parent1)
                .build();

        CommentOnComment c2 = CommentOnComment.builder()
                .id(id2)
                .caption("Reply 2")
                .parent(parent2)
                .build();

        assertNotEquals(null, c1);
        assertNotEquals(new Object(), c1);
        assertNotEquals(c1, c2);
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testToString() {
        UUID id = UUID.randomUUID();
        CommentOnPost parent = CommentOnPost.builder()
                .id(UUID.randomUUID())
                .caption("Parent Caption")
                .build();

        CommentOnComment comment = CommentOnComment.builder()
                .id(id)
                .caption("Reply")
                .parent(parent)
                .build();

        String toString = comment.toString();

        assertTrue(toString.contains("id=" + id));
        assertTrue(toString.contains("caption=Reply"));
        assertTrue(toString.contains("parent=" + parent));
    }

    @Test
    void testPrePersistWithExistingCreatedAt() {
        LocalDateTime existing = LocalDateTime.now().minusDays(1);
        CommentOnComment comment = new CommentOnComment();
        comment.setCreatedAt(existing);

        comment.onCreate();

        // Should not change the existing timestamp
        assertEquals(existing, comment.getCreatedAt());
    }

    @Test
    void testEqualsWithSameObject() {
        CommentOnComment comment = new CommentOnComment();

        assertEquals(comment, comment);
        assertEquals(comment.hashCode(), comment.hashCode());
    }

    @Test
    void testBuilderSetsDefaultCreatedAt() {
        UUID id = UUID.randomUUID();
        String caption = "Test caption";
        CommentOnPost parent = CommentOnPost.builder()
                .caption("Parent Caption")
                .build();

        LocalDateTime beforeBuild = LocalDateTime.now();
        CommentOnComment comment = CommentOnComment.builder()
                .id(id)
                .caption(caption)
                .parent(parent)
                .build();
        LocalDateTime afterBuild = LocalDateTime.now();

        assertNotNull(comment.getCreatedAt());
        // Verify the createdAt was set within the expected timeframe
        assertTrue(
                !comment.getCreatedAt().isBefore(beforeBuild) &&
                        !comment.getCreatedAt().isAfter(afterBuild.plusSeconds(1)),
                "CreatedAt should be auto-generated between beforeBuild and afterBuild");
    }
}
