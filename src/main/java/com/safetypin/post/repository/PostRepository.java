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

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCategory(String category);
    
    List<Post> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startTime AND :endTime AND p.category = :category")
    List<Post> findByTimestampBetweenAndCategory(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("category") String category);
    
    // Updated spatial query to work with H2 for testing
    @Query(value = "SELECT p.* FROM posts p " +
           "WHERE ST_DWithin(p.location, :point, :distanceMeters) = true", 
           nativeQuery = true)
    Page<Post> findPostsWithinPointAndRadius(
            @Param("point") Point point,
            @Param("distanceMeters") Double distanceMeters,
            Pageable pageable);
}
