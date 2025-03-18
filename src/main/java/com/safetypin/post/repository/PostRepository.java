package com.safetypin.post.repository;

import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByCategory(Category category);


    List<Post> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // DEPRECIATED, use findPostsWithFilter instead
    @Query(value = "SELECT p.* FROM posts p " +
            "WHERE p.created_at BETWEEN :startTime AND :endTime " +
            "AND p.category_id = :#{#category.id} ", nativeQuery = true)
    List<Post> findByTimestampBetweenAndCategory(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("category") Category category);

    // DEPRECIATED, use findPostsWithFilter instead
    // Get all posts within radius
    // Updated spatial query to work with H2 for testing
    @Query(value = "SELECT p.*, ST_Distance(p.location, :point) AS distance " +
            "FROM posts p " +
            "WHERE ST_DWithin(p.location, :point, :distanceMeters) = true " +
            "ORDER BY distance ASC, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsWithinPointAndRadius(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            Pageable pageable);


    // get posts with filter
    @Query(value = "SELECT p.*, ST_Distance(p.location, :point) AS distance " +
            "FROM posts p " +
            "WHERE ST_DWithin(p.location, :point, :distanceMeters) = true " +
            "AND p.category_id = :#{#category.id} " +
            "AND p.created_at BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY distance ASC, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsWithFilter(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("category") Category category,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);


}
