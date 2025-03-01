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

    /**
     * Find posts by category
     */
    List<Post> findByCategory(String category);
    
    /**
     * Find posts created between the specified dates
     */
    List<Post> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find posts by category and created between the specified dates
     */
    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.category = :category")
    List<Post> findByTimestampBetweenAndCategory(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate, 
                                                @Param("category") String category);
    
    /**
     * Find posts within a specified radius of a point
     */
    @Query("SELECT p FROM Post p WHERE ST_Distance(p.location, :center) <= :radius")
    Page<Post> findPostsWithinPointAndRadius(@Param("center") Point center, @Param("radius") Double radius, Pageable pageable);
}
