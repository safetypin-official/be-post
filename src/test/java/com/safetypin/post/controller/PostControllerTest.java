package com.safetypin.post.controller;

import com.safetypin.post.model.Post;
import com.safetypin.post.service.PostService;
import com.safetypin.post.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostControllerTest.TestSecurityConfig.class)
@ExtendWith(MockitoExtension.class)
public class PostControllerTest {

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @ComponentScan(basePackageClasses = PostController.class)
    public static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/**").permitAll());
            return http.build();
        }

        @Bean
        public PostController postController(PostService postService, LocationService locationService) {
            return new PostController(postService, locationService);
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private PostService postService;

    @Autowired
    private LocationService locationService;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .alwaysDo(print())
                .build();
    }

    @Test
    public void testGetPostsWithLocation() throws Exception {
        // Create proper Post objects instead of HashMaps
        List<Post> mockPosts = new ArrayList<>();

        Post post1 = new Post();
        post1.setId(1L);
        post1.setTitle("Test Post 1");
        post1.setCreatedAt(LocalDateTime.now());
        Point point1 = geometryFactory.createPoint(new Coordinate(0.001, 0.001));
        post1.setLocation(point1);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setTitle("Test Post 2");
        post2.setCreatedAt(LocalDateTime.now());
        Point point2 = geometryFactory.createPoint(new Coordinate(0.002, 0.002));
        post2.setLocation(point2);

        mockPosts.add(post1);
        mockPosts.add(post2);

        // Mock service call with proper Page implementation
        Page<Post> postPage = new PageImpl<>(mockPosts, PageRequest.of(0, 10), mockPosts.size());
        when(postService.findPostsByLocation(
                eq(0.0), eq(0.0), eq(100.0), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn((Page) postPage);

        // Execute request and verify
        mockMvc.perform(get("/posts")
                        .param("lat", "0")
                        .param("lon", "0")
                        .param("radius", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetPostsWithoutLocation() throws Exception {
        // Mock location service to return null
        when(locationService.getCurrentUserLocation()).thenReturn(null);

        // Execute request and verify response for missing location
        mockMvc.perform(get("/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Location required"));
    }

    @Test
    public void testGetPostsWithFilters() throws Exception {
        // Setup mock posts with category filter using proper Post objects
        List<Post> mockPosts = new ArrayList<>();

        Post newsPost = new Post();
        newsPost.setId(1L);
        newsPost.setTitle("News Post");
        newsPost.setCategory("News");
        newsPost.setCreatedAt(LocalDateTime.of(2023, 6, 15, 10, 30));
        Point point = geometryFactory.createPoint(new Coordinate(0.001, 0.001));
        newsPost.setLocation(point);

        mockPosts.add(newsPost);

        // Mock service call with filters
        Page<Post> postPage = new PageImpl<>(mockPosts, PageRequest.of(0, 10), mockPosts.size());
        when(postService.findPostsByLocation(
                eq(0.0), eq(0.0), eq(100.0),
                eq("News"),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2023, 12, 31, 23, 59, 59)),
                any(PageRequest.class)
        )).thenReturn((Page) postPage);

        // Execute request with filters
        mockMvc.perform(get("/posts")
                        .param("lat", "0")
                        .param("lon", "0")
                        .param("radius", "100")
                        .param("category", "News")
                        .param("dateFrom", "2023-01-01")
                        .param("dateTo", "2023-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("News"));
    }

    @Test
    public void testGetPostsWithInvalidLocation() throws Exception {
        // Execute request with invalid location params
        mockMvc.perform(get("/posts")
                        .param("lat", "abc")
                        .param("lon", "def")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid location parameters"));
    }

    @Test
    public void testGetPostsWithPagination() throws Exception {
        // Setup mock posts using proper Post objects
        List<Post> mockPosts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Post post = new Post();
            post.setId((long)i);
            post.setTitle("Test Post " + i);
            post.setCreatedAt(LocalDateTime.now());
            Point point = geometryFactory.createPoint(new Coordinate(0.001 * i, 0.001 * i));
            post.setLocation(point);
            mockPosts.add(post);
        }

        // Mock service call with pagination
        Page<Post> postPage = new PageImpl<>(mockPosts, PageRequest.of(0, 10), 20);
        when(postService.findPostsByLocation(
                eq(0.0), eq(0.0), eq(100.0), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 10))
        )).thenReturn((Page) postPage);

        // Execute request with pagination
        mockMvc.perform(get("/posts")
                        .param("lat", "0")
                        .param("lon", "0")
                        .param("radius", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    public void initialLoadWithPagination_shouldReturnOnlyTenPosts() throws Exception {
        // Create 50 mock posts
        List<Post> mockPosts = createMockPosts(50);

        // Setup paging for first 10 posts (page 0, size 10)
        Page<Post> pagedResponse = new PageImpl<>(
                mockPosts.subList(0, 10),
                PageRequest.of(0, 10),
                mockPosts.size()
        );

        // Mock the service call
        Point center = geometryFactory.createPoint(new Coordinate(0, 0));
        when(postService.searchPostsWithinRadius(
                eq(center), eq(100.0), any(Pageable.class)
        )).thenReturn((Page) pagedResponse);

        // Make request and verify
        mockMvc.perform(get("/posts")
                        .param("lat", "0")
                        .param("lon", "0")
                        .param("radius", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void subsequentLoadWithPagination_shouldReturnNextTenPosts() throws Exception {
        // Create 50 mock posts
        List<Post> mockPosts = createMockPosts(50);

        // Setup paging for next 10 posts (page 1, size 10)
        Page<Post> pagedResponse = new PageImpl<>(
                mockPosts.subList(10, 20),
                PageRequest.of(1, 10),
                mockPosts.size()
        );

        // Mock the service call
        Point center = geometryFactory.createPoint(new Coordinate(0, 0));
        when(postService.searchPostsWithinRadius(
                eq(center), eq(100.0), any(Pageable.class)
        )).thenReturn((Page) pagedResponse);

        // Make request and verify
        mockMvc.perform(get("/posts")
                        .param("lat", "0")
                        .param("lon", "0")
                        .param("radius", "100")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    public void testLocationSharingDisabled() throws Exception {
        // Mock location service to return null (no location available)
        when(locationService.getCurrentUserLocation()).thenReturn(null);

        // Execute request without location parameters
        mockMvc.perform(get("/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Location required"));
    }

    @Test
    public void testManualLocationUsed() throws Exception {
        // Setup mock posts for manual location using Post objects
        List<Post> mockPosts = new ArrayList<>();

        Post post = new Post();
        post.setId(1L);
        post.setTitle("Manual Location Post");
        post.setCreatedAt(LocalDateTime.now());
        Point point = geometryFactory.createPoint(new Coordinate(1.1, 1.1));
        post.setLocation(point);

        mockPosts.add(post);

        // Mock manual location in location service
        Point manualPoint = geometryFactory.createPoint(new Coordinate(1.0, 1.0));
        when(locationService.getCurrentUserLocation()).thenReturn(manualPoint);

        // Mock post service to return posts based on the manual location
        Page<Post> postPage = new PageImpl<>(mockPosts);
        when(postService.findPostsByLocation(
                eq(1.0), eq(1.0), eq(100.0), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn((Page) postPage);

        // Execute request without explicit location params (should use manual location)
        mockMvc.perform(get("/posts")
                        .param("radius", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Manual Location Post"));
    }

    /**
     * Helper method to create a list of mock posts
     */
    private List<Post> createMockPosts(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    double lat = 0.1 + (i * 0.01);
                    double lon = 0.1 + (i * 0.01);
                    Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

                    // Create Post object using the correct setter methods
                    Post post = new Post();
                    post.setId((long) i);
                    post.setContent("Content for post " + i);
                    post.setCreatedAt(LocalDateTime.now());
                    post.setLocation(point);

                    return post;
                })
                .collect(Collectors.toList());
    }
}