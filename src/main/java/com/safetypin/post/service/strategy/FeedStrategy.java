package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.model.Post;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface FeedStrategy {
    Page<Map<String, Object>> processFeed(List<Post> posts, FeedQueryDTO queryDTO);
}
