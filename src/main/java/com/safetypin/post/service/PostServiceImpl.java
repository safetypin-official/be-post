package com.safetypin.post.service;

import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.utils.DistanceCalculator;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final GeometryFactory geometryFactory;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, GeometryFactory geometryFactory) {
        this.postRepository = postRepository;
        this.geometryFactory = geometryFactory;
    }

    // find all (debugging purposes)
    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // find posts given filter
    @Override
    public Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius,
            Category category, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable) {

        // create point
        Point centerPoint = geometryFactory.createPoint(new Coordinate(centerLon, centerLat));

        // get all posts within radius with page
        Page<Post> posts = postRepository.findPostsWithFilter(
                centerPoint, radius, category, dateFrom, dateTo, pageable);

        // add distance to each posts
        return posts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            result.put("post", post);
            Point postLocation = post.getLocation();
            if (postLocation != null) {
                double distance = DistanceCalculator.calculateDistance(
                        centerLat, centerLon,
                        postLocation.getY(), postLocation.getX()
                );
                result.put("distance", distance);
            } else {
                result.put("distance", null);
            }
            return result;
        });
    }

    @Override
    public Post createPost(String title, String content, Double latitude, Double longitude, Category category) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidPostDataException("Post title is required");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidPostDataException("Post content is required");
        }

        if (latitude == null || longitude == null) {
            throw new InvalidPostDataException("Location coordinates are required");
        }

        try {
            Post post = Post.builder()
                    .title(title)
                    .caption(content)
                    .category(category)
                    .createdAt(LocalDateTime.now())
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            return postRepository.save(post);
        } catch (Exception e) {
            throw new PostException("Failed to create post: " + e.getMessage(), "POST_CREATION_ERROR", e);
        }
    }

}