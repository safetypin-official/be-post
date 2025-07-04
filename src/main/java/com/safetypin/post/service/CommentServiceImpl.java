package com.safetypin.post.service;

import com.safetypin.post.dto.*;
import com.safetypin.post.exception.InvalidPostDataException;
import com.safetypin.post.exception.PostNotFoundException;
import com.safetypin.post.exception.UnauthorizedAccessException;
import com.safetypin.post.model.BasePost;
import com.safetypin.post.model.CommentOnComment;
import com.safetypin.post.model.CommentOnPost;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CommentOnCommentRepository;
import com.safetypin.post.repository.CommentOnPostRepository;
import com.safetypin.post.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final PostRepository postRepository;
    private final PostService postService;
    private final CommentOnPostRepository commentOnPostRepository;
    private final CommentOnCommentRepository commentOnCommentRepository;

    // fetch all comments by postedBy
    public Page<CommentDTOWithPostId> getCommentsByPostedBy(UUID postedBy, Pageable pageable) {
        // fetch commentOnPOst & commentOnComment
        List<CommentOnPost> commentOnPosts = commentOnPostRepository.findCommentsByPostedBy(postedBy);
        List<CommentOnComment> commentOnComments = commentOnCommentRepository.findCommentsByPostedBy(postedBy);


        // fetch UUID createdBy
        List<UUID> createdByList = new ArrayList<>(commentOnPosts.stream().map(BasePost::getPostedBy).distinct().toList());
        List<UUID> createdByList2 = commentOnComments.stream().map(BasePost::getPostedBy).distinct().toList();
        createdByList.addAll(createdByList2);

        // fetch batch
        Map<UUID, PostedByData> profileList = postService.fetchPostedByData(createdByList);

        // convert to DTO
        List<CommentDTOWithPostId> commentOnPostsDTO = commentOnPosts.stream().map(commentOnPost ->
                new CommentDTOWithPostId(
                        new CommentDTO(commentOnPost, profileList.get(commentOnPost.getPostedBy())),
                        commentOnPost.getParent().getId(),
                        CommentType.COMMENT_ON_POST
                )).toList();
        List<CommentDTOWithPostId> commentOnCommentsDTO = commentOnComments.stream().map(commentOnComment ->
                new CommentDTOWithPostId(
                        new CommentDTO(commentOnComment, profileList.get(commentOnComment.getPostedBy())),
                        commentOnComment.getParent().getParent().getId(),
                        CommentType.COMMENT_ON_COMMENT
                )).toList();

        // merge
        List<CommentDTOWithPostId> comments = new ArrayList<>();
        comments.addAll(commentOnPostsDTO);
        comments.addAll(commentOnCommentsDTO);

        // sort by createdAt
        List<CommentDTOWithPostId> sortedComments = comments.stream()
                .sorted(Comparator.comparing(c -> c.getComment().getCreatedAt()))
                .toList().reversed();

        // page it
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedComments.size());

        List<CommentDTOWithPostId> pageContent = start >= sortedComments.size() ? Collections.emptyList()
                : sortedComments.subList(start, end);

        return new PageImpl<>(pageContent, pageable, sortedComments.size());
    }


    // fetch comment on post
    public Page<CommentDTO> getCommentOnPost(UUID postId, Pageable pageable) {
        if (postRepository.findById(postId).isEmpty()) {
            throw new IllegalArgumentException("PostId is not found");
        }

        // find comments
        List<BasePost> comments = new ArrayList<>(commentOnPostRepository.findByParentId(postId));

        return organizeComments(comments, pageable, true);
    }

    // fetch comment on comment
    public Page<CommentDTO> getCommentOnComment(UUID commentId, Pageable pageable) {
        if (commentOnPostRepository.findById(commentId).isEmpty()) {
            throw new IllegalArgumentException("CommentId is not found");
        }

        List<BasePost> comments = new ArrayList<>(commentOnCommentRepository.findByParentId(commentId));

        return organizeComments(comments, pageable, false);
    }

    private Page<CommentDTO> organizeComments(List<BasePost> comments, Pageable pageable, boolean sortNewToOld) {
        // get UUID createdBy
        List<UUID> createdByList = comments.stream().map(BasePost::getPostedBy).distinct().toList();

        // fetch batch
        Map<UUID, PostedByData> profileList = postService.fetchPostedByData(createdByList);

        // sort by timestamp & convert to DTO
        List<CommentDTO> sortedComments = comments.stream()
                .sorted(Comparator.comparing(BasePost::getCreatedAt))
                .map(comment -> new CommentDTO(comment, profileList.get(comment.getPostedBy())))
                .toList();

        if (sortNewToOld) {
            sortedComments = sortedComments.reversed();
        }

        // page it
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedComments.size());

        List<CommentDTO> pageContent = start >= sortedComments.size() ? Collections.emptyList()
                : sortedComments.subList(start, end);

        return new PageImpl<>(pageContent, pageable, sortedComments.size());
    }

    // create comment on post
    public CommentOnPost createCommentOnPost(CommentRequest req) {
        Post post = postRepository.findById(req.getParentId())
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        UserDetails userDetails = getUserDetails();
        UUID userId = userDetails.getUserId();

        CommentOnPost comment = CommentOnPost.builder()
                .parent(post)
                .postedBy(userId)
                .caption(req.getCaption())
                .build();

        validateCommentRequest(req, userDetails);

        return commentOnPostRepository.save(comment);
    }

    // create comment on comment
    public CommentOnComment createCommentOnComment(CommentRequest req) {
        CommentOnPost commentOnPost = commentOnPostRepository.findById(req.getParentId())
                .orElseThrow(() -> new PostNotFoundException("Comment on post not found"));

        UserDetails userDetails = getUserDetails();
        UUID userId = userDetails.getUserId();

        CommentOnComment comment = CommentOnComment.builder()
                .parent(commentOnPost)
                .postedBy(userId)
                .caption(req.getCaption())
                .build();
        validateCommentRequest(req, userDetails);

        return commentOnCommentRepository.save(comment);
    }

    @Override
    public void deleteComment(UUID commentId, UUID userId, boolean isCommentOnComment) {
        if (isCommentOnComment) {
            // Delete a child comment (CommentOnComment)
            deleteChildComment(commentId, userId);
        } else {
            // Delete a parent comment (CommentOnPost)
            deleteParentComment(commentId, userId);
        }
    }

    private void deleteParentComment(UUID commentId, UUID userId) {
        // Find the parent comment
        CommentOnPost parentComment = commentOnPostRepository.findById(commentId)
                .orElseThrow(() -> new PostNotFoundException("Comment not found"));

        // Check if the user is authorized to delete this comment
        if (!parentComment.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this comment");
        }

        // When deleting a parent comment, all child comments will be deleted
        // automatically
        // because of the CascadeType.ALL and orphanRemoval=true in the @OneToMany
        // relationship
        commentOnPostRepository.delete(parentComment);
        log.info("Deleted parent comment with ID: {} and all its child comments", commentId);
    }

    private void deleteChildComment(UUID commentId, UUID userId) {
        // Find the child comment
        CommentOnComment childComment = commentOnCommentRepository.findById(commentId)
                .orElseThrow(() -> new PostNotFoundException("Comment not found"));

        // Check if the user is authorized to delete this comment
        if (!childComment.getPostedBy().equals(userId)) {
            throw new UnauthorizedAccessException("User not authorized to delete this comment");
        }

        // Delete the child comment
        commentOnCommentRepository.delete(childComment);
        log.info("Deleted child comment with ID: {}", commentId);
    }

    private UserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetails) authentication.getPrincipal();
    }

    private void validateCommentRequest(CommentRequest req, UserDetails userDetails) {
        String caption = req.getCaption();
        if (caption == null || caption.trim().isEmpty()) {
            throw new InvalidPostDataException("Caption is required");
        }

        int commentLimit = userDetails.getCaptionCharacterLimit();
        String userType = userDetails.isPremiumUser() ? "premium" : "free";

        if (caption.length() > commentLimit) {
            throw new InvalidPostDataException("Caption exceeds the character limit of " + commentLimit +
                    " characters for " + userType + " users");
        }
    }
}
