package com.safetypin.post.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.safetypin.post.model.Post;
import com.safetypin.post.model.VoteType;

class PostDataTest {

        @Test
        void testPostDataBuilder() {
                // Given
                UUID id = UUID.randomUUID();
                String title = "Test Title";
                String caption = "Test Caption";
                Double latitude = 45.0;
                Double longitude = 90.0;
                LocalDateTime createdAt = LocalDateTime.now();
                String category = "Safety";
                Long upvoteCount = 10L;
                Long downvoteCount = 5L;
                VoteType currentVote = VoteType.UPVOTE;
                UUID userId = UUID.randomUUID();
                String name = "Hoho";
                String profilePicture = "www.google.com";
                PostedByData postedBy = PostedByData.builder()
                                .id(userId)
                                .name(name)
                                .profilePicture(profilePicture)
                                .build();

                // When
                PostData postData = PostData.builder()
                                .id(id)
                                .title(title)
                                .caption(caption)
                                .latitude(latitude)
                                .longitude(longitude)
                                .createdAt(createdAt)
                                .category(category)
                                .upvoteCount(upvoteCount)
                                .downvoteCount(downvoteCount)
                                .currentVote(currentVote)
                                .postedBy(postedBy)
                                .build();

                // Then
                assertEquals(id, postData.getId());
                assertEquals(title, postData.getTitle());
                assertEquals(caption, postData.getCaption());
                assertEquals(latitude, postData.getLatitude());
                assertEquals(longitude, postData.getLongitude());
                assertEquals(createdAt, postData.getCreatedAt());
                assertEquals(category, postData.getCategory());
                assertEquals(upvoteCount, postData.getUpvoteCount());
                assertEquals(downvoteCount, postData.getDownvoteCount());
                assertEquals(currentVote, postData.getCurrentVote());
                assertEquals(postedBy.getId(), postData.getPostedBy().getId());
        }

        @Test
        void testFromPostAndUserId() {
                // Given
                Post mockPost = Mockito.mock(Post.class);
                UUID userId = UUID.randomUUID();
                UUID postedById = UUID.randomUUID();
                UUID postId = UUID.randomUUID();

                String title = "Test Post";
                String caption = "Test Caption";
                Double latitude = 35.0;
                Double longitude = 135.0;
                LocalDateTime createdAt = LocalDateTime.now();
                String category = "Safety";
                Long upvoteCount = 20L;
                Long downvoteCount = 8L;
                VoteType currentVote = VoteType.UPVOTE;

                when(mockPost.getId()).thenReturn(postId);
                when(mockPost.getTitle()).thenReturn(title);
                when(mockPost.getCaption()).thenReturn(caption);
                when(mockPost.getLatitude()).thenReturn(latitude);
                when(mockPost.getLongitude()).thenReturn(longitude);
                when(mockPost.getCreatedAt()).thenReturn(createdAt);
                when(mockPost.getCategory()).thenReturn(category);
                when(mockPost.getUpvoteCount()).thenReturn(upvoteCount);
                when(mockPost.getDownvoteCount()).thenReturn(downvoteCount);
                when(mockPost.currentVote(userId)).thenReturn(currentVote);
                when(mockPost.getPostedBy()).thenReturn(postedById);

                // When
                PostData result = PostData.fromPostAndUserId(mockPost, userId, null);

                // Then
                assertNotNull(result);
                assertEquals(postId, result.getId());
                assertEquals(title, result.getTitle());
                assertEquals(caption, result.getCaption());
                assertEquals(latitude, result.getLatitude());
                assertEquals(longitude, result.getLongitude());
                assertEquals(createdAt, result.getCreatedAt());
                assertEquals(category, result.getCategory());
                assertEquals(upvoteCount, result.getUpvoteCount());
                assertEquals(downvoteCount, result.getDownvoteCount());
                assertEquals(currentVote, result.getCurrentVote());
                assertEquals(postedById, result.getPostedBy().getId());

                // Verify that currentVote was called with the correct userId
                Mockito.verify(mockPost).currentVote(userId);
        }

        @Test
        void testFromPostAndUserId_WithEmptyProfiles() {
                // Given
                Post mockPost = Mockito.mock(Post.class);
                UUID userId = UUID.randomUUID();
                UUID postedById = UUID.randomUUID();
                UUID postId = UUID.randomUUID();

                when(mockPost.getId()).thenReturn(postId);
                when(mockPost.getTitle()).thenReturn("Test Post");
                when(mockPost.getPostedBy()).thenReturn(postedById);

                // Empty profiles list
                List<PostedByData> emptyProfiles = new ArrayList<>();

                // When
                PostData result = PostData.fromPostAndUserId(mockPost, userId, emptyProfiles);

                // Then
                assertNotNull(result);
                assertEquals(postedById, result.getPostedBy().getId());
                assertNull(result.getPostedBy().getName());
                assertNull(result.getPostedBy().getProfilePicture());
        }

        @Test
        void testFromPostAndUserId_WithProfilesButNoMatch() {
                // Given
                Post mockPost = Mockito.mock(Post.class);
                UUID userId = UUID.randomUUID();
                UUID postedById = UUID.randomUUID();
                UUID postId = UUID.randomUUID();

                // Different UUID for the profile in the list
                UUID differentUserId = UUID.randomUUID();

                when(mockPost.getId()).thenReturn(postId);
                when(mockPost.getTitle()).thenReturn("Test Post");
                when(mockPost.getPostedBy()).thenReturn(postedById);

                // Create a profile list with a non-matching profile
                List<PostedByData> profiles = new ArrayList<>();
                profiles.add(PostedByData.builder()
                                .id(differentUserId)
                                .name("Different User")
                                .profilePicture("different-picture.jpg")
                                .build());

                // When
                PostData result = PostData.fromPostAndUserId(mockPost, userId, profiles);

                // Then
                assertNotNull(result);
                assertEquals(postedById, result.getPostedBy().getId());
                assertNull(result.getPostedBy().getName());
                assertNull(result.getPostedBy().getProfilePicture());
        }

        @Test
        void testFromPostAndUserId_WithMatchingProfile() {
                // Given
                Post mockPost = Mockito.mock(Post.class);
                UUID userId = UUID.randomUUID();
                UUID postedById = UUID.randomUUID();
                UUID postId = UUID.randomUUID();

                String profileName = "Matching User";
                String profilePicture = "matching-picture.jpg";

                when(mockPost.getId()).thenReturn(postId);
                when(mockPost.getTitle()).thenReturn("Test Post");
                when(mockPost.getPostedBy()).thenReturn(postedById);

                // Create a profile list with a matching profile
                List<PostedByData> profiles = new ArrayList<>();
                profiles.add(PostedByData.builder()
                                .id(postedById)
                                .name(profileName)
                                .profilePicture(profilePicture)
                                .build());

                // When
                PostData result = PostData.fromPostAndUserId(mockPost, userId, profiles);

                // Then
                assertNotNull(result);
                assertEquals(userId, result.getPostedBy().getId()); // Should be the userId, not postedById
                assertEquals(profileName, result.getPostedBy().getName());
                assertEquals(profilePicture, result.getPostedBy().getProfilePicture());
        }

        @Test
        void testFromPostAndUserId_WithNullValues() {
                // Given
                Post mockPost = Mockito.mock(Post.class);
                UUID userId = UUID.randomUUID();

                when(mockPost.getId()).thenReturn(null);
                when(mockPost.getTitle()).thenReturn(null);
                when(mockPost.getCaption()).thenReturn(null);
                when(mockPost.getLatitude()).thenReturn(null);
                when(mockPost.getLongitude()).thenReturn(null);
                when(mockPost.getCreatedAt()).thenReturn(null);
                when(mockPost.getCategory()).thenReturn(null);
                when(mockPost.getUpvoteCount()).thenReturn(null);
                when(mockPost.getDownvoteCount()).thenReturn(null);
                when(mockPost.currentVote(userId)).thenReturn(null);
                when(mockPost.getPostedBy()).thenReturn(null);

                // When
                PostData result = PostData.fromPostAndUserId(mockPost, userId, null);

                // Then
                assertNotNull(result);
                assertNull(result.getId());
                assertNull(result.getTitle());
                assertNull(result.getCaption());
                assertNull(result.getLatitude());
                assertNull(result.getLongitude());
                assertNull(result.getCreatedAt());
                assertNull(result.getCategory());
                assertNull(result.getUpvoteCount());
                assertNull(result.getDownvoteCount());
                assertNull(result.getCurrentVote());
                assertNull(result.getPostedBy().getId());
        }

        @Test
        void testEqualsAndHashCode() {
                // Given
                LocalDateTime now = LocalDateTime.now();
                UUID userId = UUID.randomUUID();
                UUID id = UUID.randomUUID();
                String name = "Hoho";
                String profilePicture = "www.google.com";
                PostedByData postedBy = PostedByData.builder()
                                .id(userId)
                                .name(name)
                                .profilePicture(profilePicture)
                                .build();

                PostData postData1 = PostData.builder()
                                .id(id)
                                .title("Title")
                                .caption("Caption")
                                .latitude(45.0)
                                .longitude(90.0)
                                .createdAt(now)
                                .category("Safety")
                                .upvoteCount(10L)
                                .downvoteCount(5L)
                                .currentVote(VoteType.UPVOTE)
                                .postedBy(postedBy)
                                .build();

                PostData postData2 = PostData.builder()
                                .id(id)
                                .title("Title")
                                .caption("Caption")
                                .latitude(45.0)
                                .longitude(90.0)
                                .createdAt(now)
                                .category("Safety")
                                .upvoteCount(10L)
                                .downvoteCount(5L)
                                .currentVote(VoteType.UPVOTE)
                                .postedBy(postedBy)
                                .build();

                PostData differentPostData = PostData.builder()
                                .id(UUID.randomUUID())
                                .title("Different Title")
                                .caption("Caption")
                                .latitude(45.0)
                                .longitude(90.0)
                                .createdAt(now)
                                .category("Safety")
                                .upvoteCount(10L)
                                .downvoteCount(5L)
                                .currentVote(VoteType.UPVOTE)
                                .postedBy(postedBy)
                                .build();

                // Then
                assertEquals(postData1, postData2);
                assertEquals(postData1.hashCode(), postData2.hashCode());

                assertNotEquals(postData1, differentPostData);
                assertNotEquals(postData1.hashCode(), differentPostData.hashCode());
        }

        @Test
        void testToString() {
                // Given
                LocalDateTime now = LocalDateTime.now();
                UUID userId = UUID.randomUUID();
                UUID id = UUID.randomUUID();
                String name = "Hoho";
                String profilePicture = "www.google.com";
                PostedByData postedBy = PostedByData.builder()
                                .id(userId)
                                .name(name)
                                .profilePicture(profilePicture)
                                .build();

                PostData postData = PostData.builder()
                                .id(id)
                                .title("Title")
                                .caption("Caption")
                                .latitude(45.0)
                                .longitude(90.0)
                                .createdAt(now)
                                .category("Safety")
                                .upvoteCount(10L)
                                .downvoteCount(5L)
                                .currentVote(VoteType.UPVOTE)
                                .postedBy(postedBy)
                                .build();

                // When
                String toStringResult = postData.toString();

                // Then
                assertTrue(toStringResult.contains(id.toString()));
                assertTrue(toStringResult.contains("Title"));
                assertTrue(toStringResult.contains("Caption"));
                assertTrue(toStringResult.contains("45.0"));
                assertTrue(toStringResult.contains("90.0"));
                assertTrue(toStringResult.contains(now.toString()));
                assertTrue(toStringResult.contains("Safety"));
                assertTrue(toStringResult.contains("10"));
                assertTrue(toStringResult.contains("5"));
                assertTrue(toStringResult.contains("UPVOTE"));
                assertTrue(toStringResult.contains(postedBy.toString()));
        }

}