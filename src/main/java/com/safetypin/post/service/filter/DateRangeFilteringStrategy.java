package com.safetypin.post.service.filter;

import com.safetypin.post.model.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class DateRangeFilteringStrategy implements PostFilteringStrategy {

    private final LocalDateTime startDate;
    private final LocalDateTime endDate;

    public DateRangeFilteringStrategy(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public List<Post> filter(List<Post> posts) {
        if (startDate == null || endDate == null) {
            return posts;
        }

        return posts.stream()
                .filter(post -> {
                    LocalDateTime createdAt = post.getCreatedAt();
                    return createdAt != null &&
                            (createdAt.isEqual(startDate) || createdAt.isAfter(startDate)) &&
                            (createdAt.isEqual(endDate) || createdAt.isBefore(endDate));
                })
                .collect(Collectors.toList());
    }
}
