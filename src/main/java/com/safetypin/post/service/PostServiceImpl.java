package com.safetypin.post.service;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.service.strategy.DistanceFeedStrategy;
import com.safetypin.post.service.strategy.FeedStrategy;
import com.safetypin.post.service.strategy.TimestampFeedStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final DistanceFeedStrategy distanceFeedStrategy;
    private final TimestampFeedStrategy timestampFeedStrategy;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, CategoryRepository categoryRepository,
                           DistanceFeedStrategy distanceFeedStrategy,
                           TimestampFeedStrategy timestampFeedStrategy) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.distanceFeedStrategy = distanceFeedStrategy;
        this.timestampFeedStrategy = timestampFeedStrategy;
    }

    // find all (debugging purposes)
    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // find all with pagination
    @Override
    public Page<Post> findAllPaginated(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    @Override
    public Post createPost(String title, String content, Double latitude, Double longitude, String category,
                           UUID postedBy) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidPostDataException("Title is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidPostDataException("Content is required");
        }
        if (latitude == null || longitude == null) {
            throw new InvalidPostDataException("Location coordinates are required");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidPostDataException("Category is required");
        }
        if (postedBy == null) {
            throw new InvalidPostDataException("User ID (postedBy) is required");
        }

        // Verify that the category exists
        Category categoryObj = categoryRepository.findByName(category);
        if (categoryObj == null) {
            throw new InvalidPostDataException("Category does not exist: " + category);
        }

        // Create the post
        Post post = new Post.Builder()
                .title(title)
                .caption(content)
                .location(latitude, longitude)
                .category(category)
                .postedBy(postedBy) // Set the postedBy value
                .build();

        try {
            return postRepository.save(post);
        } catch (Exception e) {
            log.error("Error saving post: {}", e.getMessage());
            throw new PostException("Failed to save the post: " + e.getMessage());
        }
    }

    @Override
    public Post findById(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + id));
    }

    @Override
    public void deletePost(UUID postId, UUID userId) {
        Post post = findById(postId);
        if (!post.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this post");
        }
        postRepository.delete(post);
    }

    @Override
    public Page<Map<String, Object>> getFeed(FeedQueryDTO queryDTO, String feedType) {
        // Validate categories if provided
        if (queryDTO.getCategories() != null && !queryDTO.getCategories().isEmpty()) {
            validateCategories(queryDTO.getCategories());
        }

        // Validate feed type
        if (feedType == null) {
            throw new IllegalArgumentException("Feed type is required");
        }

        // Choose strategy based on feed type
        FeedStrategy strategy = switch (feedType.toLowerCase()) {
            case "distance" -> distanceFeedStrategy;
            case "timestamp" -> timestampFeedStrategy;
            default -> throw new IllegalArgumentException("Invalid feed type: " + feedType);
        };

        // Get all posts
        List<Post> allPosts = postRepository.findAll();

        // Apply strategy to posts
        return strategy.processFeed(allPosts, queryDTO);
    }

    @Override
    public Page<Map<String, Object>> findPostsByUser(UUID postUserId, Pageable pageable) {
        if (postUserId == null) {
            throw new IllegalArgumentException("Post user ID is required");
        }

        // Get all posts with filters and ordered
        Page<Post> allPosts = postRepository.findByPostedByOrderByCreatedAtDesc(postUserId, pageable);

        // Map to PostData and return page
        return allPosts.map(post -> {
            Map<String, Object> result = new HashMap<>();
            PostData postData = PostData.fromPostAndUserId(post, postUserId);
            result.put("post", postData);
            return result;
        });
    }

    // Helper method to validate that all categories exist
    private void validateCategories(List<String> categories) {
        for (String category : categories) {
            Category categoryObj = categoryRepository.findByName(category);
            if (categoryObj == null) {
                throw new InvalidPostDataException("Category does not exist: " + category);
            }
        }
    }
}