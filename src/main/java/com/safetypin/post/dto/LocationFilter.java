package com.safetypin.post.dto;

import java.time.LocalDateTime;

public class LocationFilter {
    private String category;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    public LocationFilter() {
    }

    public LocationFilter(String category, LocalDateTime dateFrom, LocalDateTime dateTo) {
        this.category = category;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDateTime getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDateTime dateTo) {
        this.dateTo = dateTo;
    }
}
