package com.safetypin.post.service;

import com.safetypin.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface PostService {

    Page<Map<String, Object>> findPostsByLocation(
            Double centerLat, Double centerLon, Double radius,
            String category, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable);

    Post createPost(String title, String content, Double latitude, Double longitude, String category);

    List<Post> findAll();

    Page<Post> findAllPaginated(Pageable pageable);
}