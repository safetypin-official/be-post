package com.safetypin.post.service.filter;

import com.safetypin.post.model.Category;
import com.safetypin.post.model.Post;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryFilteringStrategy implements PostFilteringStrategy {
    
    private final Category category;
    
    public CategoryFilteringStrategy(Category category) {
        this.category = category;
    }
    
    @Override
    public List<Post> filter(List<Post> posts) {
        if (category == null) {
            return posts;
        }
        
        return posts.stream()
                .filter(post -> category.equals(post.getCategory()))
                .collect(Collectors.toList());
    }
}
