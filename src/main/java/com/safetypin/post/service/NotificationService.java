package com.safetypin.post.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.safetypin.post.dto.NotificationDto;

@Service
public interface NotificationService {
    /**
     * Retrieves notifications for a given user within the last 30 days.
     *
     * @param userId The ID of the user for whom to fetch notifications.
     * @return A list of NotificationDto objects, sorted by creation time
     *         descending.
     */
    List<NotificationDto> getNotifications(UUID userId);
}
