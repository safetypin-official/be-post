package com.safetypin.post.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.safetypin.post.model.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private NotificationType type;
    private UUID actorUserId; // The user who performed the action (commented/replied)
    private String actorName;
    private String actorProfilePictureUrl;
    private String timeAgo;
    private UUID postId;
    private UUID commentId; // ID of the parent comment (for replies or sibling replies)
    private UUID replyId; // ID of the specific reply (for NEW_REPLY* or NEW_SIBLING_REPLY)
    private LocalDateTime createdAt; // Keep original timestamp for sorting
    private String commentContent; // The content of the comment
    private String postTitle; // The title of the post
}
