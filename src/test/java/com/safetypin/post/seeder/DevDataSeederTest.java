package com.safetypin.post.seeder;

import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import com.safetypin.post.repository.CategoryRepository;
import com.safetypin.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class DevDataSeederTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private DevDataSeeder devDataSeeder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        devDataSeeder = new DevDataSeeder(postRepository, categoryRepository);
    }

    @Test
    void testRunCallsSeedMethods() {
        // Arrange
        when(categoryRepository.count()).thenReturn(0L);
        when(postRepository.count()).thenReturn(0L);

        // Act
        devDataSeeder.run();

        // Assert
        verify(categoryRepository, times(1)).count();
        verify(categoryRepository, times(1)).saveAll(anyList());
        verify(postRepository, times(1)).count();
        verify(postRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testSeedCategoriesWhenNoCategoriesExist() {
        // Arrange
        when(categoryRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Category>> categoryCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        devDataSeeder.run();

        // Assert
        verify(categoryRepository).saveAll(categoryCaptor.capture());
        List<Category> savedCategories = categoryCaptor.getValue();
        assertEquals(8, savedCategories.size());
        assertEquals("Lost Item", savedCategories.get(0).getName());
        assertEquals("Infrastructure Issue", savedCategories.get(1).getName());
        assertEquals("Crime Watch", savedCategories.get(2).getName());
        assertEquals("Lost Book", savedCategories.get(3).getName());
        assertEquals("Lost Pet", savedCategories.get(4).getName());
        assertEquals("Service Issue", savedCategories.get(5).getName());
        assertEquals("Flooding", savedCategories.get(6).getName());
        assertEquals("Stolen Vehicle", savedCategories.get(7).getName());
    }

    @Test
    void testSeedCategoriesWhenCategoriesAlreadyExist() {
        // Arrange
        when(categoryRepository.count()).thenReturn(10L);

        // Act
        devDataSeeder.run();

        // Assert
        verify(categoryRepository, times(1)).count();
        verify(categoryRepository, never()).saveAll(anyList());
    }

    @Test
    void testSeedPostsWhenNoPostsExist() {
        // Arrange
        when(categoryRepository.count()).thenReturn(10L); // Skip category seeding
        when(postRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Post>> postCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        devDataSeeder.run();

        // Assert
        verify(postRepository).saveAll(postCaptor.capture());
        List<Post> savedPosts = postCaptor.getValue();
        assertEquals(15, savedPosts.size());

        // Verify first post content
        assertEquals("Dompet saya hilang di sekitar Margonda, tolong hubungi jika menemukan!", savedPosts.get(0).getCaption());
        assertEquals("Lost wallet near Margonda", savedPosts.get(0).getTitle());
        assertEquals("Lost Item", savedPosts.get(0).getCategory());

        // Verify last post content
        assertEquals("ATM di ITC Depok error, sudah coba beberapa kali tapi kartu tidak bisa keluar. Hati-hati yang mau pakai!", savedPosts.get(5).getCaption());
        assertEquals("ATM issue at ITC Depok", savedPosts.get(5).getTitle());
        assertEquals("Service Issue", savedPosts.get(5).getCategory());
    }

    @Test
    void testSeedPostsWhenPostsAlreadyExist() {
        // Arrange
        when(categoryRepository.count()).thenReturn(10L); // Skip category seeding
        when(postRepository.count()).thenReturn(5L);

        // Act
        devDataSeeder.run();

        // Assert
        verify(postRepository, times(1)).count();
        verify(postRepository, never()).saveAll(anyList());
    }
}
