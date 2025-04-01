package com.safetypin.post.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeedQueryDTO {
    private List<String> categories;
    private String keyword;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private UUID userId;
    private Pageable pageable;

    // Only used for distance-based feed
    private Double userLat;
    private Double userLon;
}
