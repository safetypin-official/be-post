package com.safetypin.post.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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

    @Test
    void testDefaultConstructor() {
        CommentOnPost comment = new CommentOnPost();

        assertNull(comment.getId());
        assertNull(comment.getCaption());
        assertNull(comment.getCreatedAt());
        assertNull(comment.getParent());
        assertNull(comment.getPostedBy());
        assertNull(comment.getComments());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        String caption = "Test caption";
        LocalDateTime createdAt = LocalDateTime.now();
        Post parent = new Post();
        UUID postedBy = UUID.randomUUID();
        List<CommentOnComment> comments = List.of(new CommentOnComment(), new CommentOnComment());

        CommentOnPost comment = new CommentOnPost(parent, comments);
        comment.setId(id);
        comment.setCaption(caption);
        comment.setCreatedAt(createdAt);
        comment.setPostedBy(postedBy);

        assertEquals(id, comment.getId());
        assertEquals(caption, comment.getCaption());
        assertEquals(createdAt, comment.getCreatedAt());
        assertEquals(parent, comment.getParent());
        assertEquals(postedBy, comment.getPostedBy());
        assertEquals(comments, comment.getComments());
    }

    @Test
    void testBuilderWithAllFields() {
        UUID id = UUID.randomUUID();
        String caption = "Test caption";
        LocalDateTime createdAt = LocalDateTime.now();
        UUID postedBy = UUID.randomUUID();
        Post parent = Post.builder()
                .id(UUID.randomUUID())
                .title("Parent title")
                .caption("Parent caption")
                .location(0.0, 0.0)
                .build();

        CommentOnPost comment = CommentOnPost.builder()
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
    void testSetAndGetCaption() {
        CommentOnPost comment = new CommentOnPost();
        String caption = "Test caption";

        comment.setCaption(caption);

        assertEquals(caption, comment.getCaption());
    }

    @Test
    void testSetAndGetId() {
        CommentOnPost comment = new CommentOnPost();
        UUID id = UUID.randomUUID();

        comment.setId(id);

        assertEquals(id, comment.getId());
    }

    @Test
    void testSetAndGetPostedBy() {
        CommentOnPost comment = new CommentOnPost();
        UUID postedBy = UUID.randomUUID();

        comment.setPostedBy(postedBy);

        assertEquals(postedBy, comment.getPostedBy());
    }

    @Test
    void testSetAndGetCreatedAt() {
        CommentOnPost comment = new CommentOnPost();
        LocalDateTime createdAt = LocalDateTime.now();

        comment.setCreatedAt(createdAt);

        assertEquals(createdAt, comment.getCreatedAt());
    }

    @Test
    void testPrePersistWithExistingCreatedAt() {
        LocalDateTime existing = LocalDateTime.now().minusDays(1);
        CommentOnPost comment = new CommentOnPost();
        comment.setCreatedAt(existing);

        comment.onCreate();

        // The onCreate method always sets a new timestamp, it doesn't check if one
        // exists
        assertNotEquals(existing, comment.getCreatedAt());
    }

    @Test
    void testEqualsWithDifferentObjects() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Post parent1 = Post.builder().id(UUID.randomUUID()).title("Title 1").location(0.0, 0.0).build();
        Post parent2 = Post.builder().id(UUID.randomUUID()).title("Title 2").location(0.0, 0.0).build();

        CommentOnPost c1 = CommentOnPost.builder()
                .id(id1)
                .caption("Caption 1")
                .parent(parent1)
                .build();

        CommentOnPost c2 = CommentOnPost.builder()
                .id(id2)
                .caption("Caption 2")
                .parent(parent2)
                .build();

        assertNotEquals(c1, c2);
        assertNotEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(null, c1);
        assertNotEquals(new Object(), c1);
    }

    @Test
    void testEqualsWithSameObject() {
        CommentOnPost comment = new CommentOnPost();

        assertEquals(comment.hashCode(), comment.hashCode());
    }

    @Test
    void testPrePersistBehavior() {
        // Verify that the @PrePersist annotation works correctly
        // by checking that the method correctly sets createdAt
        CommentOnPost comment = new CommentOnPost();
        assertNull(comment.getCreatedAt());

        // Direct invocation of the method that has the @PrePersist annotation
        comment.onCreate();

        // The method should always set a new timestamp, regardless of previous value
        LocalDateTime createdAt = comment.getCreatedAt();
        assertNotNull(createdAt);
        assertTrue(createdAt.isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(createdAt.isAfter(LocalDateTime.now().minusSeconds(10)));

        // Test that the method always sets a new timestamp, even if one already exists
        LocalDateTime oldTimestamp = comment.getCreatedAt();

        // Wait a small amount of time to ensure timestamps would be different
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            fail("Test was interrupted");
        }

        comment.onCreate();
        LocalDateTime newTimestamp = comment.getCreatedAt();

        // The timestamps should be different because CommentOnPost always sets a new
        // timestamp
        // without checking if one already exists
        assertNotEquals(oldTimestamp, newTimestamp);
    }

    @Test
    void testToString() {
        UUID id = UUID.randomUUID();
        String caption = "Test caption";
        LocalDateTime createdAt = LocalDateTime.now();
        UUID postedBy = UUID.randomUUID();
        Post parent = new Post();
        parent.setId(UUID.randomUUID());

        CommentOnPost comment = CommentOnPost.builder()
                .id(id)
                .caption(caption)
                .createdAt(createdAt)
                .postedBy(postedBy)
                .parent(parent)
                .build();

        String toString = comment.toString();

        // Verify toString contains essential fields
        assertTrue(toString.contains("CommentOnPost"));
        assertTrue(toString.contains("parent"));
    }

    @Test
    void testEqualsWithDifferentCommentsList() {
        UUID id = UUID.randomUUID();
        Post parent = new Post();
        parent.setId(UUID.randomUUID());

        CommentOnComment comment1 = new CommentOnComment();
        CommentOnComment comment2 = new CommentOnComment();

        List<CommentOnComment> commentList1 = new ArrayList<>();
        commentList1.add(comment1);

        List<CommentOnComment> commentList2 = new ArrayList<>();
        commentList2.add(comment2);

        CommentOnPost post1 = CommentOnPost.builder()
                .id(id)
                .caption("Test caption")
                .parent(parent)
                .build();
        post1.setComments(commentList1);

        CommentOnPost post2 = CommentOnPost.builder()
                .id(id)
                .caption("Test caption")
                .parent(parent)
                .build();
        post2.setComments(commentList2);

        // Since comments are transient, they don't affect equality
        assertEquals(post1, post2);
        assertEquals(post1.hashCode(), post2.hashCode());
    }

    @Test
    void testNullCommentsList() {
        CommentOnPost comment = new CommentOnPost();
        assertNull(comment.getComments());

        comment.setComments(null);
        assertNull(comment.getComments());

        List<CommentOnComment> emptyList = new ArrayList<>();
        comment.setComments(emptyList);
        assertEquals(emptyList, comment.getComments());
    }

    @Test
    void testBuilderTogetherWithSetter() {
        UUID id = UUID.randomUUID();
        Post parent = new Post();

        CommentOnPost comment = CommentOnPost.builder()
                .id(id)
                .build();

        comment.setParent(parent);

        assertEquals(id, comment.getId());
        assertEquals(parent, comment.getParent());
    }
}
