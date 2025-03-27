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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private final String testCategoryName = "Test Category";
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryServiceImpl categoryService;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setName(testCategoryName);
    }

    @Test
    void testCreateCategory_Success() {
        // Arrange
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            return invocation.<Category>getArgument(0);
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
        Category category1 = new Category("Category 1");
        Category category2 = new Category("Category 2");
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
    void testUpdateCategoryName_Success() {
        // Arrange
        String newCategoryName = "Updated Category";
        Category updatedCategory = new Category(newCategoryName);
        updatedCategory.setDescription(testCategory.getDescription());
        
        when(categoryRepository.findByName(testCategoryName)).thenReturn(testCategory);
        when(categoryRepository.existsById(newCategoryName)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
        
        // Act
        Category result = categoryService.updateCategoryName(testCategoryName, newCategoryName);
        
        // Assert
        assertNotNull(result);
        assertEquals(newCategoryName, result.getName());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, times(1)).existsById(newCategoryName);
        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(categoryRepository, times(1)).delete(testCategory);
    }
    
    @Test
    void testUpdateCategoryName_CategoryNotFound() {
        // Arrange
        String newCategoryName = "Updated Category";
        when(categoryRepository.findByName(testCategoryName)).thenReturn(null);
        
        // Act & Assert
        CategoryException exception = assertThrows(CategoryException.class, 
            () -> categoryService.updateCategoryName(testCategoryName, newCategoryName));
        assertEquals("Category not found", exception.getMessage());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, never()).existsById(any());
        verify(categoryRepository, never()).save(any());
        verify(categoryRepository, never()).delete(any());
    }
    
    @Test
    void testUpdateCategoryName_NewNameAlreadyExists() {
        // Arrange
        String newCategoryName = "Existing Category";
        
        when(categoryRepository.findByName(testCategoryName)).thenReturn(testCategory);
        when(categoryRepository.existsById(newCategoryName)).thenReturn(true);
        
        // Act & Assert
        CategoryException exception = assertThrows(CategoryException.class, 
            () -> categoryService.updateCategoryName(testCategoryName, newCategoryName));
        assertEquals("Category with name " + newCategoryName + " already exists", exception.getMessage());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, times(1)).existsById(newCategoryName);
        verify(categoryRepository, never()).save(any());
        verify(categoryRepository, never()).delete(any());
    }
    
    @Test
    void testUpdateCategoryName_SameNameProvided() {
        // Arrange
        Category updatedCategory = new Category(testCategoryName);
        updatedCategory.setDescription(testCategory.getDescription());
        
        when(categoryRepository.findByName(testCategoryName)).thenReturn(testCategory);
        when(categoryRepository.existsById(testCategoryName)).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
        
        // Act
        Category result = categoryService.updateCategoryName(testCategoryName, testCategoryName);
        
        // Assert
        assertNotNull(result);
        assertEquals(testCategoryName, result.getName());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, times(1)).existsById(testCategoryName);
        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(categoryRepository, never()).delete(any());
    }
}
