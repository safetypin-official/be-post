package com.safetypin.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedRequestDTO {
    private List<String> categories;
    private String keyword;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private int page;
    private int size;

    // Only for distance-based feed
    private Double lat;
    private Double lon;
    private Double radius;
}
