package com.safetypin.post.service;

import com.safetypin.post.exception.CategoryException;
import com.safetypin.post.model.Category;
import com.safetypin.post.repository.CategoryRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private final UUID testUuid = UUID.randomUUID();
    private final String testCategoryName = "Test Category";

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(testUuid);
        testCategory.setName(testCategoryName);
    }

    @Test
    void testCreateCategory_Success() {
        // Arrange
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(testUuid);
            return category;
        });

        // Act
        Category result = categoryService.createCategory(testCategoryName);

        // Assert
        assertNotNull(result);
        assertEquals(testCategoryName, result.getName());
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
    }

    @Test
    void testCreateCategory_ThrowsException() {
        // Arrange
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new ConstraintViolationException("Constraint violation", null));

        // Act & Assert
        assertThrows(CategoryException.class, () -> categoryService.createCategory(testCategoryName));
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
    }

    @Test
    void testGetCategoryByName_Success() {
        // Arrange
        when(categoryRepository.findByName(testCategoryName)).thenReturn(testCategory);

        // Act
        Category result = categoryService.getCategoryByName(testCategoryName);

        // Assert
        assertNotNull(result);
        assertEquals(testCategoryName, result.getName());
        assertEquals(testUuid, result.getId());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
    }
    
    @Test
    void testGetCategoryByName_NotFound() {
        // Arrange
        when(categoryRepository.findByName("Nonexistent")).thenReturn(null);

        // Act
        Category result = categoryService.getCategoryByName("Nonexistent");

        // Assert
        assertNull(result);
        verify(categoryRepository, times(1)).findByName("Nonexistent");
    }

    @Test
    void testGetAllCategories() {
        // Arrange
        Category category1 = new Category(UUID.randomUUID(), "Category 1");
        Category category2 = new Category(UUID.randomUUID(), "Category 2");
        List<Category> expectedCategories = Arrays.asList(category1, category2);
        
        when(categoryRepository.findAll()).thenReturn(expectedCategories);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedCategories, result);
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void testUpdateCategory_Success() {
        // Arrange
        String updatedName = "Updated Category";
        Category updatedCategory = new Category(testUuid, updatedName);
        
        when(categoryRepository.findById(testUuid)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        // Act
        Category result = categoryService.updateCategory(updatedCategory);

        // Assert
        assertNotNull(result);
        assertEquals(updatedName, result.getName());
        verify(categoryRepository, times(1)).findById(testUuid);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void testUpdateCategory_NotFound() {
        // Arrange
        when(categoryRepository.findById(testUuid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CategoryException.class, () -> categoryService.updateCategory(testCategory));
        verify(categoryRepository, times(1)).findById(testUuid);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testDeleteCategory_Success() {
        // Arrange
        when(categoryRepository.existsById(testUuid)).thenReturn(true);
        doNothing().when(categoryRepository).delete(testCategory);

        // Act
        categoryService.deleteCategory(testCategory);

        // Assert
        verify(categoryRepository, times(1)).existsById(testUuid);
        verify(categoryRepository, times(1)).delete(testCategory);
    }

    @Test
    void testDeleteCategory_NotFound() {
        // Arrange
        when(categoryRepository.existsById(testUuid)).thenReturn(false);

        // Act & Assert
        assertThrows(CategoryException.class, () -> categoryService.deleteCategory(testCategory));
        verify(categoryRepository, times(1)).existsById(testUuid);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void testDeleteCategoryByName_Success() {
        // Arrange
        when(categoryRepository.findByName(testCategoryName)).thenReturn(testCategory);
        doNothing().when(categoryRepository).delete(testCategory);

        // Act
        categoryService.deleteCategoryByName(testCategoryName);

        // Assert
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, times(1)).delete(testCategory);
    }

    @Test
    void testDeleteCategoryByName_NotFound() {
        // Arrange
        when(categoryRepository.findByName(testCategoryName)).thenReturn(null);

        // Act & Assert
        assertThrows(CategoryException.class, () -> categoryService.deleteCategoryByName(testCategoryName));
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
