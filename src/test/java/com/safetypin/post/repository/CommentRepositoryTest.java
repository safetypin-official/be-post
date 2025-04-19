package com.safetypin.post.repository;

import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class CommentRepositoryTest {

    @Autowired
    private CommentOnPostRepository commentOnPostRepository;

    @Autowired
    private PostRepository postRepository;

    private GeometryFactory geometryFactory;

    private Post post;
    private CommentOnPost commentOnPost;

    @BeforeEach
    void setup() {
        // Clear previous data
        commentOnPostRepository.deleteAll();
        postRepository.deleteAll();

        // Initialize geometry factory for spatial operations
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // Create a test post
        post = new Post();
        post.setTitle("Test Post");
        post.setCaption("Test Caption");
        post.setPostedBy(UUID.randomUUID());
        post.setLocation(geometryFactory.createPoint(new Coordinate(-6.2088, 106.8456)));
        post.setCategory("example category"); // Add this line to set the required name field
        post = postRepository.save(post);

        // Create a test comment
        commentOnPost = new CommentOnPost();
        commentOnPost.setCaption("Test Comment");
        commentOnPost.setParent(post);
        commentOnPost.setPostedBy(UUID.randomUUID());
        commentOnPost.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateComment() {
        CommentOnPost savedComment = commentOnPostRepository.save(commentOnPost);
        assertNotNull(savedComment.getId());
    }

    @Test
    void testFindComment() {
        commentOnPostRepository.save(commentOnPost);
        assertTrue(commentOnPostRepository.findById(commentOnPost.getId()).isPresent());
    }

    @Test
    void testDeleteComment() {
        commentOnPostRepository.save(commentOnPost);
        commentOnPostRepository.delete(commentOnPost);
        assertTrue(commentOnPostRepository.findById(commentOnPost.getId()).isEmpty());
    }

    @Test
    void testFindByParentId() {
        commentOnPostRepository.save(commentOnPost);
        assertFalse(commentOnPostRepository.findByParentId(post.getId()).isEmpty());
    }
}
