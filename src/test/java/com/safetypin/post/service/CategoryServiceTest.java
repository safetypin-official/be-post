package com.safetypin.post.service;

import com.safetypin.post.exception.CategoryException;
import com.safetypin.post.model.Category;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private final String testCategoryName = "Test Category";
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
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
    void testUpdateCategoryName_SameNames() {
        // Arrange
        when(categoryRepository.findByName(testCategoryName)).thenReturn(testCategory);

        // Act
        Category result = categoryService.updateCategoryName(testCategoryName, testCategoryName);

        // Assert
        assertNotNull(result);
        assertEquals(testCategoryName, result.getName());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, never()).saveAndFlush(any());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testUpdateCategoryName_Success() {
        // Arrange
        String newCategoryName = "New Category";
        Category oldCategory = new Category(testCategoryName);

        Category newCategory = new Category(newCategoryName);

        when(categoryRepository.findByName(testCategoryName)).thenReturn(oldCategory);
        when(categoryRepository.existsById(newCategoryName)).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(newCategory);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(5); // Assume 5 posts updated

        // Act
        Category result = categoryService.updateCategoryName(testCategoryName, newCategoryName);

        // Assert
        assertNotNull(result);
        assertEquals(newCategoryName, result.getName());
        verify(categoryRepository, times(1)).findByName(testCategoryName);
        verify(categoryRepository, times(1)).existsById(newCategoryName);
        verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
        verify(categoryRepository, times(1)).delete(oldCategory);
        verify(entityManager, times(1)).createNativeQuery(anyString());
        verify(query, times(2)).setParameter(anyString(), any());
        verify(query, times(1)).executeUpdate();
    }
}
