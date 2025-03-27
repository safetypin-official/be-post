package com.safetypin.post.service;

import com.safetypin.post.exception.CategoryException;
import com.safetypin.post.model.Category;
import com.safetypin.post.repository.CategoryRepository;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
    public Category updateCategoryName(String oldCategoryName, String newCategoryName) throws CategoryException {
        Category category = getCategoryByName(oldCategoryName);
        
        if (category == null) {
            log.error("Category with name: {}, not found", oldCategoryName);
            throw new CategoryException("Category not found");
        }
        
        // Check if new name already exists and is different from old name
        if (categoryRepository.existsById(newCategoryName) && !oldCategoryName.equals(newCategoryName)) {
            throw new CategoryException("Category with name " + newCategoryName + " already exists");
        }
        
        // Create new category with new name but same description
        Category newCategory = new Category(newCategoryName);
        newCategory.setDescription(category.getDescription());
        
        // Save new category
        categoryRepository.save(newCategory);
        
        // Delete old category if names are different
        if (!oldCategoryName.equals(newCategoryName)) {
            categoryRepository.delete(category);
        }
        
        return newCategory;
    }
}
