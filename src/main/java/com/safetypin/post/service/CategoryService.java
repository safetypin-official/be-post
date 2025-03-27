package com.safetypin.post.service;

import com.safetypin.post.exception.CategoryException;
import com.safetypin.post.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CategoryService {

    Category createCategory(String categoryName);

    Category getCategoryByName(String categoryName);

    List<Category> getAllCategories();

    Category updateCategoryName(String oldCategoryName, String newCategoryName) throws CategoryException;
}
