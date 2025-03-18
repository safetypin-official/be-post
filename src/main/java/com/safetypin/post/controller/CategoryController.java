package com.safetypin.post.controller;


import com.safetypin.post.dto.PostResponse;
import com.safetypin.post.model.Category;
import com.safetypin.post.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<PostResponse> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(new PostResponse(
                true, "Categories found", categories
        ));
    }

    @PostMapping("/create")
    public ResponseEntity<PostResponse> createCategory(
            @RequestParam String categoryName
    ) {
        Category createdCategory;

        try {
            createdCategory = categoryService.createCategory(categoryName);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(new PostResponse(
                    false, "Category creation failed", e.getMessage()
            ));
        }

        return ResponseEntity.ok(new PostResponse(
                true, "Category created", createdCategory
        ));
    }

    @PostMapping("/update")
    public ResponseEntity<PostResponse> updateCategory(
            @RequestBody Category category
    ) {
        Category updatedCategory;

        try {
            updatedCategory = categoryService.updateCategory(category);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(new PostResponse(
                    false, "Category update failed", e.getMessage()
            ));
        }

        return ResponseEntity.ok(new PostResponse(
                true, "Category updated to " + category, updatedCategory
        ));
    }


    @DeleteMapping("/delete")
    public ResponseEntity<PostResponse> deleteCategory(
            @RequestParam Category category
    ) {
        try {
            categoryService.deleteCategory(category);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(new PostResponse(
                    false, "Category deletion failed", e.getMessage()
            ));
        }

        return ResponseEntity.ok(new PostResponse(
                true, "Category " + category.getName() + " deleted", null
        ));
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PostResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        PostResponse errorResponse = new PostResponse(
                false, "Invalid category parameters", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
