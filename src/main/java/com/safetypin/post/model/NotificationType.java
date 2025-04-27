package com.safetypin.post.model;

public enum NotificationType {
    NEW_COMMENT_ON_POST, // Someone commented on your post
    NEW_REPLY_TO_COMMENT, // Someone replied to your comment
    NEW_SIBLING_REPLY // Someone else replied to the same comment thread you are in
}
