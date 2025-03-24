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

    // get posts with filter - Updated to use name instead of category_id
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND (:category IS NULL OR p.name = :category) " +
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

    // Filter by category only - Simplified since name is in posts table directly
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND p.name = :category " +
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

    // Filter by both category and dates - Simplified
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND p.name = :category " +
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

    // Search posts by keyword in title or caption
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "    to_tsvector('english', p.title || ' ' || p.caption) @@ plainto_tsquery('english', :keyword)) " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> searchPostsByKeyword(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Filter posts by categories
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND p.name IN :categories " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> searchPostsByCategories(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("categories") List<String> categories,
            Pageable pageable);

    // Search posts by keyword and filter by categories
    @Query(value = "SELECT p.*, " +
            "CASE WHEN p.location IS NOT NULL THEN ST_Distance(p.location, :point) ELSE NULL END AS distance " +
            "FROM posts p " +
            "WHERE (p.location IS NULL OR ST_DWithin(p.location, :point, :distanceMeters) = true) " +
            "AND to_tsvector('english', p.title || ' ' || p.caption) @@ plainto_tsquery('english', :keyword) " +
            "AND p.name IN :categories " +
            "ORDER BY distance ASC NULLS LAST, p.id ASC",
            nativeQuery = true)
    Page<Post> searchPostsByKeywordAndCategories(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            @Param("keyword") String keyword,
            @Param("categories") List<String> categories,
            Pageable pageable);
}
