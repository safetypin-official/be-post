package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.FeedQueryDTO;
import com.safetypin.post.dto.PostData;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.model.Post;
import com.safetypin.post.utils.DistanceCalculator;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DistanceFeedStrategy extends AbstractFeedStrategy {
    private static final String DISTANCE_KEY = "distance";


    @Override
    public Page<Map<String, Object>> processFeed(List<Post> posts, FeedQueryDTO queryDTO, Map<UUID, PostedByData> profileList) {
        if (queryDTO.getUserLat() == null || queryDTO.getUserLon() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required for distance feed");
        }

        List<Map<String, Object>> postsWithDistance = posts.stream()
                .filter(post -> matchesCategories(post, queryDTO.getCategories()))
                .filter(post -> matchesKeyword(post, queryDTO.getKeyword()))
                .filter(post -> matchesDateRange(post, queryDTO.getDateFrom(), queryDTO.getDateTo()))
                .map(post -> {
                    Map<String, Object> result = new HashMap<>();

                    PostData postData = (profileList == null) ? null : PostData.fromPostAndUserId(post, queryDTO.getUserId(), profileList.get(post.getPostedBy()));
                    result.put("post", postData);

                    // Calculate distance from user
                    double distance = DistanceCalculator.calculateDistance(
                            queryDTO.getUserLat(), queryDTO.getUserLon(),
                            post.getLatitude(), post.getLongitude());
                    result.put(DISTANCE_KEY, distance);

                    return result;
                })
                // Sort by distance (nearest first)
                .sorted(Comparator.comparingDouble(post -> (Double) post.get(DISTANCE_KEY)))
                .toList();

        return paginateResults(postsWithDistance, queryDTO.getPageable());
    }
}
