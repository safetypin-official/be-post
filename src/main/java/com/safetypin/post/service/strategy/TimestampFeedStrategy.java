package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TimestampFeedStrategy extends AbstractFeedStrategy {


    @Override
    public Page<Map<String, Object>> processFeed(List<Post> posts, FeedQueryDTO queryDTO, List<PostedByData> profileList) {
        List<Map<String, Object>> filteredPosts = posts.stream()
                .filter(post -> matchesCategories(post, queryDTO.getCategories()))
                .filter(post -> matchesKeyword(post, queryDTO.getKeyword()))
                .filter(post -> matchesDateRange(post, queryDTO.getDateFrom(), queryDTO.getDateTo()))
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();
                    PostData postData = PostData.fromPostAndUserId(post, queryDTO.getUserId(), profileList);
                    result.put("post", postData);
                    return result;
                })
                // Sort by timestamp (newest first)
                .sorted(Comparator.comparing(
                        (Map<String, Object> post) -> ((PostData) post.get("post")).getCreatedAt()).reversed())
                .toList();

        return paginateResults(filteredPosts, queryDTO.getPageable());
    }
}
