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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void getAllCategories_Success() {
        // Setup
        Category category1 = new Category();
        category1.setName("Category 1");

        Category category2 = new Category();
        category2.setName("Category 2");

        List<Category> categories = Arrays.asList(category1, category2);

        when(categoryService.getAllCategories()).thenReturn(categories);

        // Execute
        ResponseEntity<PostResponse> response = categoryController.getAllCategories();

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Categories retrieved successfully", response.getBody().getMessage());

        List<String> responseData = (List<String>) response.getBody().getData();
        assertEquals(2, responseData.size());
        assertEquals("Category 1", responseData.get(0));
        assertEquals("Category 2", responseData.get(1));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    void getAllCategories_Exception() {
        // Setup
        when(categoryService.getAllCategories()).thenThrow(new RuntimeException("Test error"));

        // Execute
        ResponseEntity<PostResponse> response = categoryController.getAllCategories();

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Error retrieving categories"));
        assertNull(response.getBody().getData());

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    void createCategory_Success() {
        // Setup
        Category category = new Category();
        category.setName("TestCategory");

        when(categoryService.createCategory("TestCategory")).thenReturn(category);

        // Execute
        ResponseEntity<PostResponse> response = categoryController.createCategory("TestCategory");

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Category created", response.getBody().getMessage());
        assertEquals(category, response.getBody().getData());

        verify(categoryService, times(1)).createCategory("TestCategory");
    }

    @Test
    void createCategory_Exception() {
        // Setup
        when(categoryService.createCategory("TestCategory")).thenThrow(new RuntimeException("Test error"));

        // Execute
        ResponseEntity<PostResponse> response = categoryController.createCategory("TestCategory");

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Category creation failed", response.getBody().getMessage());
        assertEquals("Test error", response.getBody().getData());

        verify(categoryService, times(1)).createCategory("TestCategory");
    }

    @Test
    void updateCategory_Success() {
        // Setup
        Category category = new Category();
        category.setName("UpdatedCategory");

        when(categoryService.updateCategory(category)).thenReturn(category);

        // Execute
        ResponseEntity<PostResponse> response = categoryController.updateCategory(category);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Category updated"));
        assertEquals(category, response.getBody().getData());

        verify(categoryService, times(1)).updateCategory(category);
    }

    @Test
    void updateCategory_Exception() {
        // Setup
        Category category = new Category();
        category.setName("UpdatedCategory");

        when(categoryService.updateCategory(category)).thenThrow(new RuntimeException("Test error"));

        // Execute
        ResponseEntity<PostResponse> response = categoryController.updateCategory(category);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Category update failed", response.getBody().getMessage());
        assertEquals("Test error", response.getBody().getData());

        verify(categoryService, times(1)).updateCategory(category);
    }

    @Test
    void deleteCategory_Success() {
        // Setup
        doNothing().when(categoryService).deleteCategoryByName("TestCategory");

        // Execute
        ResponseEntity<PostResponse> response = categoryController.deleteCategory("TestCategory");

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Category TestCategory deleted", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(categoryService, times(1)).deleteCategoryByName("TestCategory");
    }

    @Test
    void deleteCategory_Exception() {
        // Setup
        doThrow(new RuntimeException("Test error")).when(categoryService).deleteCategoryByName("TestCategory");

        // Execute
        ResponseEntity<PostResponse> response = categoryController.deleteCategory("TestCategory");

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Category deletion failed", response.getBody().getMessage());
        assertEquals("Test error", response.getBody().getData());

        verify(categoryService, times(1)).deleteCategoryByName("TestCategory");
    }

    @Test
    void handleArgumentTypeMismatch_ReturnsErrorResponse() {
        // Setup
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

        // Execute
        ResponseEntity<PostResponse> response = categoryController.handleArgumentTypeMismatch(exception);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());

        PostResponse body = response.getBody();
        assertFalse(body.isSuccess());
        assertEquals("Invalid category parameters", body.getMessage());
        assertNull(body.getData());
    }
}
