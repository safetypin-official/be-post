package com.safetypin.post.service.filter;

import com.safetypin.post.model.Post;
import java.util.ArrayList;
import java.util.List;

public class CompositeFilteringStrategy implements PostFilteringStrategy {
    
    private final List<PostFilteringStrategy> strategies = new ArrayList<>();
    
    public CompositeFilteringStrategy() {}

    public void addStrategy(PostFilteringStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
        }
    }
    
    @Override
    public List<Post> filter(List<Post> posts) {
        List<Post> result = new ArrayList<>(posts);
        
        for (PostFilteringStrategy strategy : strategies) {
            result = strategy.filter(result);
        }
        
        return result;
    }
}
