package com.safetypin.post.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.safetypin.post.dto.NotificationDto;
import com.safetypin.post.dto.PostedByData; // Using PostedByData as it seems to be the existing DTO for profile info
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.NotificationType;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

        private final CommentOnPostRepository commentOnPostRepository;
        private final CommentOnCommentRepository commentOnCommentRepository;
        private final RestTemplate restTemplate;
        private final PostRepository postRepository;

        @Value("${be-auth}") // Use a base URL property
        private String authServiceBaseUrl;

        private static final String PROFILE_BATCH_PATH = "/api/profiles/batch";

        @Override
        public List<NotificationDto> getNotifications(UUID userId) {
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                Set<UUID> actorIds = new HashSet<>();

                // 1. NEW_COMMENT_ON_POST: Someone commented on your post
                List<CommentOnPost> commentsOnUserPosts = commentOnPostRepository.findCommentsOnUserPostsSince(userId,
                                thirtyDaysAgo);
                commentsOnUserPosts.forEach(c -> actorIds.add(c.getPostedBy()));

                // 2. NEW_REPLY_TO_COMMENT: Someone replied to your comment (your comment is
                // CommentOnPost)
                List<CommentOnComment> repliesToUserComments = commentOnCommentRepository
                                .findRepliesToUserCommentsSince(userId,
                                                thirtyDaysAgo);
                repliesToUserComments.forEach(r -> actorIds.add(r.getPostedBy()));

                // 3. NEW_SIBLING_REPLY: Someone else replied to the same comment thread you are
                // in
                List<CommentOnComment> userReplies = commentOnCommentRepository
                                .findByPostedByAndCreatedAtGreaterThanEqual(userId, thirtyDaysAgo);
                List<UUID> parentCommentIdsUserRepliedTo = userReplies.stream()
                                .map(reply -> reply.getParent().getId())
                                .distinct()
                                .toList();

                List<CommentOnComment> siblingReplies = Collections.emptyList();
                if (!parentCommentIdsUserRepliedTo.isEmpty()) {
                        siblingReplies = commentOnCommentRepository.findSiblingRepliesSince(userId,
                                        parentCommentIdsUserRepliedTo,
                                        thirtyDaysAgo);
                        siblingReplies.forEach(r -> actorIds.add(r.getPostedBy()));
                }

                // Fetch user info for all actors in bulk using the correct endpoint and method
                Map<UUID, PostedByData> userInfoMap = fetchUserDetailsBatch(new ArrayList<>(actorIds));

                // Map to DTOs
                Stream<NotificationDto> commentsOnPostNotifications = commentsOnUserPosts.stream()
                                .map(comment -> createNotificationDto(
                                                NotificationType.NEW_COMMENT_ON_POST,
                                                comment.getPostedBy(),
                                                userInfoMap.get(comment.getPostedBy()),
                                                comment.getCreatedAt(),
                                                comment.getParent().getId(), // postId
                                                comment.getId(), // commentId
                                                null // replyId
                                ));

                Stream<NotificationDto> repliesToCommentNotifications = repliesToUserComments.stream()
                                .map(reply -> createNotificationDto(
                                                NotificationType.NEW_REPLY_TO_COMMENT,
                                                reply.getPostedBy(),
                                                userInfoMap.get(reply.getPostedBy()),
                                                reply.getCreatedAt(),
                                                reply.getParent().getParent().getId(), // postId
                                                reply.getParent().getId(), // commentId (parent of the reply)
                                                reply.getId() // replyId
                                ));

                Stream<NotificationDto> siblingReplyNotifications = siblingReplies.stream()
                                .map(reply -> createNotificationDto(
                                                NotificationType.NEW_SIBLING_REPLY,
                                                reply.getPostedBy(),
                                                userInfoMap.get(reply.getPostedBy()),
                                                reply.getCreatedAt(),
                                                reply.getParent().getParent().getId(), // postId
                                                reply.getParent().getId(), // commentId (parent of the reply)
                                                reply.getId() // replyId
                                ));

                // Combine, sort, and return
                return Stream.of(commentsOnPostNotifications, repliesToCommentNotifications, siblingReplyNotifications)
                                .flatMap(Function.identity())
                                .sorted(Comparator.comparing(NotificationDto::getCreatedAt).reversed())
                                .toList();
        }

        // Main method that creates a notification DTO
        private NotificationDto createNotificationDto(NotificationType type, UUID actorId, PostedByData actorInfo,
                        LocalDateTime createdAt, UUID postId, UUID commentId, UUID replyId) {
                String actorName = actorInfo != null ? actorInfo.getName() : "Unknown User";
                String actorProfilePic = actorInfo != null ? actorInfo.getProfilePicture() : null;

                // Get comment content and post title
                String commentContent = getCommentContent(type, commentId, replyId);
                String postTitle = getPostTitle(type, postId, replyId);

                return NotificationDto.builder()
                                .type(type)
                                .actorUserId(actorId)
                                .actorName(actorName)
                                .actorProfilePictureUrl(actorProfilePic)
                                .timeAgo(calculateDaysAgo(createdAt))
                                .postId(postId)
                                .commentId(commentId)
                                .replyId(replyId)
                                .createdAt(createdAt)
                                .commentContent(commentContent)
                                .postTitle(postTitle)
                                .build();
        }

        // Get the comment content based on notification type
        private String getCommentContent(NotificationType type, UUID commentId, UUID replyId) {
                if (type == NotificationType.NEW_COMMENT_ON_POST && commentId != null) {
                        // Get comment content from CommentOnPost
                        var commentOptional = commentOnPostRepository.findById(commentId);
                        if (commentOptional.isPresent()) {
                                return commentOptional.get().getCaption();
                        }
                } else if ((type == NotificationType.NEW_REPLY_TO_COMMENT || type == NotificationType.NEW_SIBLING_REPLY)
                                && replyId != null) {
                        // Get reply content from CommentOnComment
                        var replyOptional = commentOnCommentRepository.findById(replyId);
                        if (replyOptional.isPresent()) {
                                return replyOptional.get().getCaption();
                        }
                }
                return null;
        }

        // Get the post title based on notification type
        private String getPostTitle(NotificationType type, UUID postId, UUID replyId) {
                if (type == NotificationType.NEW_COMMENT_ON_POST && postId != null) {
                        var postOptional = postRepository.findById(postId);
                        if (postOptional.isPresent()) {
                                return postOptional.get().getTitle();
                        }
                } else if ((type == NotificationType.NEW_REPLY_TO_COMMENT || type == NotificationType.NEW_SIBLING_REPLY)
                                && replyId != null) {
                        var replyOptional = commentOnCommentRepository.findById(replyId);
                        if (replyOptional.isPresent()) {
                                CommentOnComment reply = replyOptional.get();
                                CommentOnPost parentComment = reply.getParent();
                                if (parentComment != null && parentComment.getParent() != null) {
                                        return parentComment.getParent().getTitle();
                                }
                        }
                }
                return null;
        }

        // Updated method to fetch user details using POST /api/profiles/batch
        private Map<UUID, PostedByData> fetchUserDetailsBatch(List<UUID> userIds) {
                if (userIds == null || userIds.isEmpty()) {
                        return Collections.emptyMap();
                }

                String uri = authServiceBaseUrl + PROFILE_BATCH_PATH;
                HttpEntity<List<UUID>> entity = new HttpEntity<>(userIds, null); // Send list of UUIDs in the body

                try {
                        ResponseEntity<Map<UUID, PostedByData>> response = restTemplate.exchange(
                                        uri,
                                        HttpMethod.POST, // Use POST method
                                        entity,
                                        new ParameterizedTypeReference<Map<UUID, PostedByData>>() {
                                        }); // Expecting Map<UUID, PostedByData>

                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                log.info("Fetched {} profiles successfully via POST {}.", response.getBody().size(),
                                                uri);
                                return response.getBody();
                        } else {
                                log.error("Failed to fetch user details from Auth service via POST {}. Status: {}, Body: {}",
                                                uri,
                                                response.getStatusCode(), response.getBody());
                        }
                } catch (ResourceAccessException e) {
                        log.error("Network error fetching profiles via POST {} for user IDs {}: {}", uri, userIds,
                                        e.getMessage());
                } catch (Exception e) {
                        log.error("Error fetching profiles via POST {} for user IDs {}: {}", uri, userIds,
                                        e.getMessage(), e);
                }
                return Collections.emptyMap(); // Return empty map on failure
        }

        // Calculate days ago
        private String calculateDaysAgo(LocalDateTime pastTime) {
                long days = ChronoUnit.DAYS.between(pastTime.toLocalDate(), LocalDateTime.now().toLocalDate());
                if (days == 0) {
                        return "Today";
                } else if (days == 1) {
                        return "1 day ago";
                } else {
                        return days + " days ago";
                }
        }
}
