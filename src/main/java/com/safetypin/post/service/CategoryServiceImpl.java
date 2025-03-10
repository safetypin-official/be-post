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
    public Category getCategoryByName(String categoryName)  {
        return categoryRepository.findByName(categoryName);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category updateCategory(Category category) throws CategoryException {
        Optional<Category> toUpdate = categoryRepository.findById(category.getId());

        if (toUpdate.isPresent()) {
            Category updatedCategory = toUpdate.get();
            updatedCategory.setName(category.getName());
            return categoryRepository.save(updatedCategory);
        } else {
            log.error("Category with id: {}, not found", category.getId());
            throw new CategoryException("Category not found");
        }
    }

    @Override
    public void deleteCategory(Category category) throws CategoryException {
        if (!categoryRepository.existsById(category.getId())) {
            throw new CategoryException("Category with id: " + category.getId() + " not found");
        }

        categoryRepository.delete(category);
        log.info("Category: {}; deleted", category);
    }
}
