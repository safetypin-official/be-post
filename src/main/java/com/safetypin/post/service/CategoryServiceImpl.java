package com.safetypin.post.service;

import com.safetypin.post.exception.CategoryException;
import com.safetypin.post.model.Category;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, PostRepository postRepository,
                               EntityManager entityManager) {
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Category createCategory(String categoryName) {
        Category category = new Category(categoryName);
        log.info("Creating category: {}", category);
        try {
            category = categoryRepository.saveAndFlush(category);
        } catch (ConstraintViolationException e) {
            log.warn("CategoryService.createCategory:: Category failed to save because; {}", e.getMessage());
            throw new CategoryException(e.getMessage());
        }

        return category;
    }

    @Override
    public Category getCategoryByName(String categoryName) {
        return categoryRepository.findByName(categoryName);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public Category updateCategoryName(String oldCategoryName, String newCategoryName) throws CategoryException {
        Category oldCategory = getCategoryByName(oldCategoryName);

        if (oldCategory == null) {
            log.error("Category with name: {}, not found", oldCategoryName);
            throw new CategoryException("Category not found");
        }


        // If it's the same name, no update needed
        if (oldCategoryName.equals(newCategoryName)) {
            return oldCategory;
        }
        
        // Check if new name already exists and is different from old name
        if (categoryRepository.existsById(newCategoryName)) {
            throw new CategoryException("Category with name " + newCategoryName + " already exists");
        }

        // First create and save the new category
        Category newCategory = new Category(newCategoryName);
        newCategory.setDescription(oldCategory.getDescription());

        // Save new category first
        categoryRepository.saveAndFlush(newCategory);

        // Use a native query to update all posts - this avoids the entity relationship
        // issues
        int updatedCount = entityManager.createNativeQuery(
                        "UPDATE posts SET name = :newCategory WHERE name = :oldCategory")
                .setParameter("newCategory", newCategoryName)
                .setParameter("oldCategory", oldCategoryName)
                .executeUpdate();

        log.info("Updated {} posts from category '{}' to '{}'",
                updatedCount, oldCategoryName, newCategoryName);

        // Delete the old category
        categoryRepository.delete(oldCategory);

        return newCategory;
    }
}
