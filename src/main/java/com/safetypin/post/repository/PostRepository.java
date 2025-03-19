package com.safetypin.post.repository;

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
    List<Post> findByCategory(String category);

    List<Post> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // DEPRECIATED, use findPostsWithFilter instead
    // Get all posts within radius
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
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND (:category IS NULL OR p.category_id = CAST(:category AS uuid)) " +
            "AND ((:dateFrom IS NULL OR :dateTo IS NULL) OR " + 
            "     p.created_at BETWEEN :dateFrom AND :dateTo) " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsWithFilter(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("category") String category,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    // Filter by category only - Join with categories table
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "JOIN categories c ON p.category_id = c.id " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND c.name = :category " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsByCategory(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("category") String category,
            Pageable pageable);

    // Filter by dates only
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND p.created_at BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsByDateRange(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    // Filter by both category and dates - Join with categories table
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "JOIN categories c ON p.category_id = c.id " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND c.name = :category " +
            "AND p.created_at BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsByCategoryAndDateRange(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("category") String category,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    // Find posts with category
    @Query(value = "SELECT p.*, ST_Distance(p.location, :point) AS distance " +
            "FROM posts p " +
            "WHERE ST_DWithin(p.location, :point, :distanceMeters) = true " +
            "ORDER BY distance ASC, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsWithinRadius(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            Pageable pageable);

    // Find posts with date range
    @Query(value = "SELECT p.*, ST_Distance(p.location, :point) AS distance " +
            "FROM posts p " +
            "WHERE ST_DWithin(p.location, :point, :distanceMeters) = true " +
            "AND p.created_at BETWEEN :dateFrom AND :dateTo " +
            "ORDER BY distance ASC, p.id ASC",
            nativeQuery = true)
    Page<Post> findPostsWithinRadiusByDateRange(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
}
