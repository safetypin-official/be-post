package com.safetypin.post.service.filter;

import com.safetypin.post.model.Post;

import java.util.List;

/**
 * Strategy interface for filtering posts based on different criteria.
 */
public interface PostFilteringStrategy {
    /**
     * Apply filtering criteria to the list of posts.
     *
     * @param posts List of posts to filter
     * @return Filtered list of posts
     */
    List<Post> filter(List<Post> posts);
}
