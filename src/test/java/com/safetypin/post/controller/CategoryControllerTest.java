package com.safetypin.post.controller;

import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.model.Category;
import com.safetypin.post.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllCategories() {
        List<Category> categories = Arrays.asList(new Category("Tech"), new Category("Health"));
        when(categoryService.getAllCategories()).thenReturn(categories);

        ResponseEntity<PostResponse> response = categoryController.getAllCategories();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Categories retrieved successfully", response.getBody().getMessage());
        assertEquals(Arrays.asList("Tech", "Health"), response.getBody().getData());
    }

    @Test
    void testFailedGetAllCategories() {
        List<Category> categories = Arrays.asList(new Category("Tech"), new Category("Health"));
        when(categoryService.getAllCategories()).thenThrow(new RuntimeException("Error A"));

        ResponseEntity<PostResponse> response = categoryController.getAllCategories();

        assertEquals(500, response.getStatusCode().value());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Error retrieving categories: Error A", response.getBody().getMessage());
        assertNull(response.getBody().getData());

    }

    @Test
    void testCreateCategory() {
        String categoryName = "Travel";
        Category category = new Category(categoryName);
        when(categoryService.createCategory(categoryName)).thenReturn(category);

        ResponseEntity<PostResponse> response = categoryController.createCategory(categoryName);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Category created", response.getBody().getMessage());
        assertEquals(category, response.getBody().getData());
    }

    @Test
    void testFailedToCreateCategory() {
        String categoryName = "Travel";
        Category category = new Category(categoryName);
        when(categoryService.createCategory(categoryName)).thenThrow(new RuntimeException("Error B"));

        ResponseEntity<PostResponse> response = categoryController.createCategory(categoryName);

        assertEquals(400, response.getStatusCode().value());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Category creation failed", response.getBody().getMessage());
        assertEquals("Error B", response.getBody().getData());
    }

    @Test
    void testUpdateCategory() {
        String oldCategoryName = "Tech";
        String newCategoryName = "Technology";
        Category updatedCategory = new Category(newCategoryName);
        when(categoryService.updateCategoryName(oldCategoryName, newCategoryName)).thenReturn(updatedCategory);

        ResponseEntity<PostResponse> response = categoryController.updateCategory(oldCategoryName, newCategoryName);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Category renamed successfully", response.getBody().getMessage());
        assertEquals(updatedCategory, response.getBody().getData());
    }

    @Test
    void testFailedToUpdateCategory() {
        String oldCategoryName = "Tech";
        String newCategoryName = "Technology";
        Category updatedCategory = new Category(newCategoryName);
        when(categoryService.updateCategoryName(oldCategoryName, newCategoryName)).thenThrow(new RuntimeException("Error C"));

        ResponseEntity<PostResponse> response = categoryController.updateCategory(oldCategoryName, newCategoryName);

        assertEquals(400, response.getStatusCode().value());
        assertFalse(Objects.requireNonNull(response.getBody()).isSuccess());
        assertEquals("Category rename failed", response.getBody().getMessage());
        assertEquals("Error C", response.getBody().getData());
    }

    @Test
    void testHandleArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

        ResponseEntity<PostResponse> response = categoryController.handleArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid category parameters", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
