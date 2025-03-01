package com.safetypin.post.repository;

import com.safetypin.post.model.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PostRepositoryTests {

    @Autowired
    private PostRepository postRepository;
    
    private final GeometryFactory geometryFactory = new GeometryFactory();
    
    @Test
    public void findPostsWithinRadius_shouldReturnOnlyPostsWithinSpecifiedDistance() {
        // Create center point at (0,0)
        Point center = geometryFactory.createPoint(new Coordinate(0, 0));
        
        // Create posts at various locations
        Post post1 = createPostWithLocation("Post within 50km", 0.3, 0.3);  // ~47km from center
        Post post2 = createPostWithLocation("Post within 100km", 0.6, 0.6); // ~94km from center
        Post post3 = createPostWithLocation("Post outside radius", 1.0, 1.0); // ~157km from center
        
        // Save all posts
        postRepository.saveAll(List.of(post1, post2, post3));

         Pageable pageable = PageRequest.of(0, 10);
        
        // Find posts within radius (100km)
        List<Post> postsInRadius = postRepository.findPostsWithinPointAndRadius(center, 100.0, pageable).getContent();
        
        // Assert that only posts within radius are returned
        assertThat(postsInRadius).hasSize(2);
        assertThat(postsInRadius.stream().map(Post::getContent))
                .containsExactlyInAnyOrder("Post within 50km", "Post within 100km");
        assertThat(postsInRadius.stream().map(Post::getContent))
                .doesNotContain("Post outside radius");
    }
    
    private Post createPostWithLocation(String content, double lat, double lng) {
        Point location = geometryFactory.createPoint(new Coordinate(lng, lat));
        Post post = new Post();
        post.setContent(content);
        post.setLocation(location);
        return post;
    }
}
