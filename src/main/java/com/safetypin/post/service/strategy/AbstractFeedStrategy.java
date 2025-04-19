package com.safetypin.post.service.strategy;

import com.safetypin.post.model.Post;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@NoArgsConstructor
public abstract class AbstractFeedStrategy implements FeedStrategy {

    // Common utility methods
    protected boolean matchesCategories(Post post, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        return post.getCategory() != null && categories.contains(post.getCategory());
    }

    protected boolean matchesKeyword(Post post, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String lowercaseKeyword = keyword.toLowerCase();
        return (post.getTitle() != null && post.getTitle().toLowerCase().contains(lowercaseKeyword)) ||
                (post.getCaption() != null && post.getCaption().toLowerCase().contains(lowercaseKeyword));
    }

    protected boolean matchesDateRange(Post post, LocalDateTime dateFrom, LocalDateTime dateTo) {
        LocalDateTime createdAt = post.getCreatedAt();
        boolean matchesFromDate = dateFrom == null || !createdAt.isBefore(dateFrom);
        boolean matchesToDate = dateTo == null || !createdAt.isAfter(dateTo);
        return matchesFromDate && matchesToDate;
    }

    protected Page<Map<String, Object>> paginateResults(List<Map<String, Object>> results, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());

        List<Map<String, Object>> pageContent = start >= results.size() ? Collections.emptyList()
                : results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }
}
