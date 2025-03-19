package com.safetypin.post.seeder;

import com.safetypin.post.model.Post;
import com.safetypin.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DevDataSeederTest {
    
    @Mock
    private PostRepository postRepository;
    
    private DevDataSeeder devDataSeeder;
    
    @Captor
    private ArgumentCaptor<List<Post>> postListCaptor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        devDataSeeder = new DevDataSeeder(postRepository);
    }
    
    @Test
    void run_shouldSkipSeedingWhenDataExists() throws Exception {
        // Arrange
        when(postRepository.count()).thenReturn(5L);
        
        // Act
        devDataSeeder.run();
        
        // Assert
        verify(postRepository, times(1)).count();
        verify(postRepository, never()).saveAll(any());
    }
    
    @Test
    void run_shouldSeedDataWhenRepositoryIsEmpty() throws Exception {
        // Arrange
        when(postRepository.count()).thenReturn(0L);
        
        // Act
        devDataSeeder.run();
        
        // Assert
        verify(postRepository, times(1)).count();
        verify(postRepository, times(1)).saveAll(postListCaptor.capture());
        
        List<Post> capturedPosts = postListCaptor.getValue();
        assertEquals(6, capturedPosts.size());
        
        // Verify first post data
        Post firstPost = capturedPosts.get(0);
        assertEquals("Dompet saya hilang di sekitar Margonda, tolong hubungi jika menemukan!", firstPost.getCaption());
        assertEquals("Lost wallet near Margonda", firstPost.getTitle());
        assertEquals("Lost Item", firstPost.getCategory());
        assertEquals(-6.381832, firstPost.getLatitude());
        assertEquals(106.832512, firstPost.getLongitude());
        
        // Verify last post data
        Post lastPost = capturedPosts.get(5);
        assertEquals("ATM di ITC Depok error, sudah coba beberapa kali tapi kartu tidak bisa keluar. Hati-hati yang mau pakai!", lastPost.getCaption());
        assertEquals("ATM issue at ITC Depok", lastPost.getTitle());
        assertEquals("Service Issue", lastPost.getCategory());
        assertEquals(-6.383420, lastPost.getLatitude());
        assertEquals(106.822142, lastPost.getLongitude());
    }
}
