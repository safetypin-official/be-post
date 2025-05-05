package com.safetypin.post.service.strategy;

import com.safetypin.post.dto.NotificationDto;
import com.safetypin.post.dto.PostedByData;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.NotificationType;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import com.safetypin.post.service.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private CommentOnPostRepository commentOnPostRepository;
    @Mock
    private CommentOnCommentRepository commentOnCommentRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID testUserId;
    private UUID actor1Id;
    private UUID actor2Id;
    private UUID postId;
    private LocalDateTime now;
    private Post post;
    private CommentOnPost parentComment; // User's comment

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        actor1Id = UUID.randomUUID();
        actor2Id = UUID.randomUUID();
        postId = UUID.randomUUID();
        UUID commentOnPostId = UUID.randomUUID(); // ID of the user's comment
        now = LocalDateTime.now();

        // Set base URL via reflection as it's @Value injected
        ReflectionTestUtils.setField(notificationService, "authServiceBaseUrl", "http://fake-auth-service");

        // Common entities
        post = new Post();
        post.setId(postId);
        post.setPostedBy(testUserId); // Post owned by the user

        parentComment = new CommentOnPost();
        parentComment.setId(commentOnPostId);
        parentComment.setPostedBy(testUserId); // Comment owned by the user
        parentComment.setParent(post);
        parentComment.setCreatedAt(now.minusDays(5));
    }

    // Helper to create CommentOnPost
    private CommentOnPost createCommentOnPost(UUID id, UUID actorId, Post parentPost, LocalDateTime createdAt) {
        CommentOnPost comment = new CommentOnPost();
        comment.setId(id);
        comment.setPostedBy(actorId);
        comment.setParent(parentPost);
        comment.setCreatedAt(createdAt);
        return comment;
    }

    // Helper to create CommentOnComment
    private CommentOnComment createCommentOnComment(UUID id, UUID actorId, CommentOnPost parentComment,
                                                    LocalDateTime createdAt) {
        CommentOnComment reply = new CommentOnComment();
        reply.setId(id);
        reply.setPostedBy(actorId);
        reply.setParent(parentComment);
        reply.setCreatedAt(createdAt);
        return reply;
    }

    // Helper to mock the successful auth service response
    private void mockAuthServiceResponse(Map<UUID, PostedByData> responseMap) {
        ResponseEntity<Map<UUID, PostedByData>> responseEntity = new ResponseEntity<>(responseMap,
                HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://fake-auth-service/api/profiles/batch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class), // Match any HttpEntity containing the list of UUIDs
                any(ParameterizedTypeReference.class))) // Match the specific ParameterizedTypeReference
                .thenReturn(responseEntity);
    }

    // Helper to mock auth service failure
    private void mockAuthServiceFailure(Exception exception) {
        when(restTemplate.exchange(
                eq("http://fake-auth-service/api/profiles/batch"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception);
    }

    @Test
    void getNotifications_AllTypes_Success() {
        // Arrange
        // 1. Comment on user's post (actor1)
        CommentOnPost commentOnUserPost = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(1));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(commentOnUserPost));

        // 2. Reply to user's comment (actor2)
        CommentOnComment replyToUserComment = createCommentOnComment(UUID.randomUUID(), actor2Id, parentComment,
                now.minusDays(2));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(List.of(replyToUserComment));

        // 3. Sibling reply (actor1 replies to same comment user replied to)
        CommentOnComment userReply = createCommentOnComment(UUID.randomUUID(), testUserId, parentComment,
                now.minusDays(4)); // User replied first
        CommentOnComment siblingReply = createCommentOnComment(UUID.randomUUID(), actor1Id, parentComment,
                now.minusDays(3)); // Actor1 replied later
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(List.of(userReply)); // User has replied
        when(commentOnCommentRepository.findSiblingRepliesSince(eq(testUserId),
                eq(List.of(parentComment.getId())),
                any(LocalDateTime.class)))
                .thenReturn(List.of(siblingReply)); // Actor1's reply is found

        // Mock comment and post content retrieval
        when(commentOnPostRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(commentOnUserPost));
        when(commentOnCommentRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(siblingReply));
        when(postRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(post));

        // Set up post title for testing
        post.setTitle("Test Post Title");
        commentOnUserPost.setCaption("Test Comment Content");
        siblingReply.setCaption("Test Reply Content");

        // Mock Auth Service response for actor1 and actor2
        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        userInfoMap.put(actor2Id, new PostedByData(actor2Id, "Actor Two", "pic2.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(3, notifications.size());
        // Verify sorting (most recent first) and content
        // Notification 1: Comment on Post (actor1, 1 day ago)
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.getFirst().getActorUserId());
        assertEquals("Actor One", notifications.getFirst().getActorName());
        assertEquals("pic1.jpg", notifications.getFirst().getActorProfilePictureUrl());
        assertEquals("1 day ago", notifications.getFirst().getTimeAgo());
        assertEquals(postId, notifications.getFirst().getPostId());
        assertEquals(commentOnUserPost.getId(), notifications.get(0).getCommentId());
        assertNull(notifications.get(0).getReplyId());

        // Notification 2: Reply to Comment (actor2, 2 days ago)
        assertEquals(NotificationType.NEW_REPLY_TO_COMMENT, notifications.get(1).getType());
        assertEquals(actor2Id, notifications.get(1).getActorUserId());
        assertEquals("Actor Two", notifications.get(1).getActorName());
        assertEquals("pic2.jpg", notifications.get(1).getActorProfilePictureUrl());
        assertEquals("2 days ago", notifications.get(1).getTimeAgo());
        assertEquals(postId, notifications.get(1).getPostId());
        assertEquals(parentComment.getId(), notifications.get(1).getCommentId());
        assertEquals(replyToUserComment.getId(), notifications.get(1).getReplyId());

        // Notification 3: Sibling Reply (actor1, 3 days ago)
        assertEquals(NotificationType.NEW_SIBLING_REPLY, notifications.get(2).getType());
        assertEquals(actor1Id, notifications.get(2).getActorUserId());
        assertEquals("Actor One", notifications.get(2).getActorName()); // Reused info
        assertEquals("pic1.jpg", notifications.get(2).getActorProfilePictureUrl());
        assertEquals("3 days ago", notifications.get(2).getTimeAgo());
        assertEquals(postId, notifications.get(2).getPostId());
        assertEquals(parentComment.getId(), notifications.get(2).getCommentId());
        assertEquals(siblingReply.getId(), notifications.get(2).getReplyId());

        // Verify repository calls
        verify(commentOnPostRepository, times(1)).findCommentsOnUserPostsSince(eq(testUserId),
                any(LocalDateTime.class));
        verify(commentOnCommentRepository, times(1)).findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class));
        verify(commentOnCommentRepository, times(1)).findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class));
        verify(commentOnCommentRepository, times(1)).findSiblingRepliesSince(eq(testUserId),
                eq(List.of(parentComment.getId())), any(LocalDateTime.class));
        // Verify auth service call
        verify(restTemplate, times(1)).exchange(
                eq("http://fake-auth-service/api/profiles/batch"),
                eq(HttpMethod.POST),
                argThat(entity -> entity != null && entity.getBody() instanceof List
                        && ((List<?>) entity.getBody())
                        .containsAll(List.of(actor1Id, actor2Id))),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_OnlyCommentsOnPost() {
        // Arrange
        CommentOnPost comment1 = createCommentOnPost(UUID.randomUUID(), actor1Id, post, now.minusDays(1));
        CommentOnPost comment2 = createCommentOnPost(UUID.randomUUID(), actor2Id, post, now.minusDays(5));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(comment1, comment2));
        // No replies or sibling replies
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        // findSiblingRepliesSince won't be called if findByPostedBy... returns empty

        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        userInfoMap.put(actor2Id, new PostedByData(actor2Id, "Actor Two", "pic2.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Mock comment and post content retrieval
        when(commentOnPostRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(comment1));
        when(postRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(post));

        // Set up post title and comment content for testing
        post.setTitle("Test Post Title");
        comment1.setCaption("Test Comment Content");

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(2, notifications.size());
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.get(0).getActorUserId());
        assertEquals("1 day ago", notifications.get(0).getTimeAgo());
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.get(1).getType());
        assertEquals(actor2Id, notifications.get(1).getActorUserId());
        assertEquals("5 days ago", notifications.get(1).getTimeAgo());

        verify(commentOnCommentRepository, never()).findSiblingRepliesSince(any(), any(), any());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_OnlyRepliesToComment() {
        // Arrange
        CommentOnComment reply1 = createCommentOnComment(UUID.randomUUID(), actor1Id, parentComment,
                now.minusDays(1));
        CommentOnComment reply2 = createCommentOnComment(UUID.randomUUID(), actor2Id, parentComment,
                now.minusDays(5));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(List.of(reply1, reply2));
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        userInfoMap.put(actor2Id, new PostedByData(actor2Id, "Actor Two", "pic2.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Mock comment and post content retrieval
        lenient().when(commentOnCommentRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(reply1));
        lenient().when(postRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(post));

        // Set up post title and comment content for testing
        post.setTitle("Test Post Title");
        reply1.setCaption("Test Reply Content");

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(2, notifications.size());
        assertEquals(NotificationType.NEW_REPLY_TO_COMMENT, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.get(0).getActorUserId());
        assertEquals("1 day ago", notifications.get(0).getTimeAgo());
        assertEquals(NotificationType.NEW_REPLY_TO_COMMENT, notifications.get(1).getType());
        assertEquals(actor2Id, notifications.get(1).getActorUserId());
        assertEquals("5 days ago", notifications.get(1).getTimeAgo());

        verify(commentOnCommentRepository, never()).findSiblingRepliesSince(any(), any(), any());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_OnlySiblingReplies() {
        // Arrange
        CommentOnComment userReply = createCommentOnComment(UUID.randomUUID(), testUserId, parentComment,
                now.minusDays(4));
        CommentOnComment siblingReply1 = createCommentOnComment(UUID.randomUUID(), actor1Id, parentComment,
                now.minusDays(1));
        CommentOnComment siblingReply2 = createCommentOnComment(UUID.randomUUID(), actor2Id, parentComment,
                now.minusDays(3));

        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(List.of(userReply)); // User has replied
        when(commentOnCommentRepository.findSiblingRepliesSince(eq(testUserId),
                eq(List.of(parentComment.getId())),
                any(LocalDateTime.class)))
                .thenReturn(List.of(siblingReply1, siblingReply2)); // Found sibling replies

        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        userInfoMap.put(actor2Id, new PostedByData(actor2Id, "Actor Two", "pic2.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(2, notifications.size());
        assertEquals(NotificationType.NEW_SIBLING_REPLY, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.get(0).getActorUserId());
        assertEquals("1 day ago", notifications.get(0).getTimeAgo());
        assertEquals(NotificationType.NEW_SIBLING_REPLY, notifications.get(1).getType());
        assertEquals(actor2Id, notifications.get(1).getActorUserId());
        assertEquals("3 days ago", notifications.get(1).getTimeAgo());

        verify(commentOnCommentRepository, times(1)).findSiblingRepliesSince(eq(testUserId),
                eq(List.of(parentComment.getId())), any(LocalDateTime.class));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_NoNotificationsFound() {
        // Arrange
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // No actors, so auth service shouldn't be called

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertTrue(notifications.isEmpty());
        verify(commentOnCommentRepository, never()).findSiblingRepliesSince(any(), any(), any());
        verify(restTemplate, never()).exchange(anyString(), any(), any(),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_AuthServiceFails_ResourceAccessException() {
        // Arrange: Setup one notification source
        CommentOnPost commentOnUserPost = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(1));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(commentOnUserPost));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Mock Auth Service failure
        mockAuthServiceFailure(new ResourceAccessException("Network error"));

        // Mock comment and post content retrieval
        when(commentOnPostRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(commentOnUserPost));
        when(postRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(post));

        // Set up post title and comment content for testing
        post.setTitle("Test Post Title");
        commentOnUserPost.setCaption("Test Comment Content");

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert: Notification should still be created but with default user info
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.getFirst().getActorUserId());
        assertEquals("Unknown User", notifications.getFirst().getActorName()); // Defaulted
        assertNull(notifications.getFirst().getActorProfilePictureUrl()); // Defaulted
        assertEquals("1 day ago", notifications.getFirst().getTimeAgo());

        // Verify auth service was called
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_AuthServiceFails_OtherException() {
        // Arrange: Setup one notification source
        CommentOnPost commentOnUserPost = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(1));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(commentOnUserPost));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Mock Auth Service failure
        mockAuthServiceFailure(new RuntimeException("Unexpected error"));

        // Add mock for PostRepository and CommentOnPostRepository
        when(commentOnPostRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(commentOnUserPost));
        when(postRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(post));

        // Set up post title and comment content for testing
        post.setTitle("Test Post Title");
        commentOnUserPost.setCaption("Test Comment Content");

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert: Notification should still be created but with default user info
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.getFirst().getActorUserId());
        assertEquals("Unknown User", notifications.getFirst().getActorName());
        assertNull(notifications.getFirst().getActorProfilePictureUrl());
        assertEquals("1 day ago", notifications.getFirst().getTimeAgo());

        // Verify auth service was called
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_AuthServiceReturnsPartialData() {
        // Arrange
        CommentOnPost comment1 = createCommentOnPost(UUID.randomUUID(), actor1Id, post, now.minusDays(1)); // Actor
        // 1
        // exists
        // in
        // auth
        // response
        CommentOnPost comment2 = createCommentOnPost(UUID.randomUUID(), actor2Id, post, now.minusDays(2)); // Actor
        // 2
        // doesn't
        // exist
        // in
        // auth
        // response
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(comment1, comment2));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Mock Auth Service - only returns info for actor1Id
        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(2, notifications.size());
        // Notification for comment1 (actor1) - Should have full info
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.getFirst().getActorUserId());
        assertEquals("Actor One", notifications.getFirst().getActorName());
        assertEquals("pic1.jpg", notifications.get(0).getActorProfilePictureUrl());
        assertEquals("1 day ago", notifications.get(0).getTimeAgo());
        // Notification for comment2 (actor2) - Should have default info
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.get(1).getType());
        assertEquals(actor2Id, notifications.get(1).getActorUserId());
        assertEquals("Unknown User", notifications.get(1).getActorName()); // Defaulted
        assertNull(notifications.get(1).getActorProfilePictureUrl()); // Defaulted
        assertEquals("2 days ago", notifications.get(1).getTimeAgo());

        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_AuthServiceReturnsEmptyMap() {
        // Arrange
        CommentOnPost commentOnUserPost = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(1));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(commentOnUserPost));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Mock Auth Service returning empty map
        mockAuthServiceResponse(Collections.emptyMap());

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert: Notification created with default info
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.getFirst().getType());
        assertEquals(actor1Id, notifications.getFirst().getActorUserId());
        assertEquals("Unknown User", notifications.getFirst().getActorName());
        assertNull(notifications.getFirst().getActorProfilePictureUrl());
        assertEquals("1 day ago", notifications.getFirst().getTimeAgo());

        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void getNotifications_TimeAgoCalculation() {
        // Arrange
        CommentOnPost commentToday = createCommentOnPost(UUID.randomUUID(), actor1Id, post, now.minusHours(1)); // Today
        CommentOnPost commentYesterday = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(1).minusHours(1)); // Yesterday
        CommentOnPost commentTwoDaysAgo = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(2).minusHours(1)); // 2 days ago
        CommentOnPost commentThirtyDaysAgo = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(30).minusHours(1)); // 30 days ago

        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(commentToday, commentYesterday, commentTwoDaysAgo,
                        commentThirtyDaysAgo));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(4, notifications.size());
        // Sorted by date descending
        assertEquals("Today", notifications.get(0).getTimeAgo());
        assertEquals("1 day ago", notifications.get(1).getTimeAgo());
        assertEquals("2 days ago", notifications.get(2).getTimeAgo());
        assertEquals("30 days ago", notifications.get(3).getTimeAgo());
    }

    @Test
    void getNotifications_SiblingReply_UserHasNoReplies_ShouldNotQuerySiblings() {
        // Arrange
        // User has *not* replied in the last 30 days
        when(commentOnCommentRepository.findByPostedByAndCreatedAtGreaterThanEqual(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Setup other notification types to ensure they are still processed
        CommentOnPost commentOnUserPost = createCommentOnPost(UUID.randomUUID(), actor1Id, post,
                now.minusDays(1));
        when(commentOnPostRepository.findCommentsOnUserPostsSince(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(List.of(commentOnUserPost));
        CommentOnComment replyToUserComment = createCommentOnComment(UUID.randomUUID(), actor2Id, parentComment,
                now.minusDays(2));
        when(commentOnCommentRepository.findRepliesToUserCommentsSince(eq(testUserId),
                any(LocalDateTime.class)))
                .thenReturn(List.of(replyToUserComment));

        // Mock Auth Service
        Map<UUID, PostedByData> userInfoMap = new HashMap<>();
        userInfoMap.put(actor1Id, new PostedByData(actor1Id, "Actor One", "pic1.jpg"));
        userInfoMap.put(actor2Id, new PostedByData(actor2Id, "Actor Two", "pic2.jpg"));
        mockAuthServiceResponse(userInfoMap);

        // Act
        List<NotificationDto> notifications = notificationService.getNotifications(testUserId);

        // Assert
        assertEquals(2, notifications.size()); // Only comment and reply notifications
        assertEquals(NotificationType.NEW_COMMENT_ON_POST, notifications.get(0).getType());
        assertEquals(NotificationType.NEW_REPLY_TO_COMMENT, notifications.get(1).getType());

        // Crucially, verify findSiblingRepliesSince was *not* called
        verify(commentOnCommentRepository, never()).findSiblingRepliesSince(any(), any(), any());
        // Verify auth service was still called for the other actors
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }
}