package com.safetypin.post.repository;

import com.safetypin.post.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findById(UUID id);

    Category findByName(String categoryName);
}
