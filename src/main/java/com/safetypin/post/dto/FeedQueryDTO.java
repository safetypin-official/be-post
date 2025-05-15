package com.safetypin.post.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private Double radius;

    // Static converter method
    public static FeedQueryDTO fromFeedRequestAndUserId(FeedRequestDTO request, UUID userId) {
        // Convert LocalDate to LocalDateTime if provided
        LocalDateTime fromDateTime = request.getDateFrom() != null
                ? LocalDateTime.of(request.getDateFrom(), LocalTime.MIN)
                : null;
        LocalDateTime toDateTime = request.getDateTo() != null ? LocalDateTime.of(request.getDateTo(), LocalTime.MAX)
                : null;

        // Set up pagination
        Pageable page = PageRequest.of(request.getPage(), request.getSize());

        return FeedQueryDTO.builder()
                .categories(request.getCategories())
                .keyword(request.getKeyword())
                .dateFrom(fromDateTime)
                .dateTo(toDateTime)
                .userId(userId)
                .pageable(page)
                .userLat(request.getLat())
                .userLon(request.getLon())
                .radius(request.getRadius())
                .build();
    }
}
