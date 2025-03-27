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
@RequestMapping("/posts/category")
public class CategoryController {
        private final CategoryService categoryService;

        public CategoryController(CategoryService categoryService) {
                this.categoryService = categoryService;
        }

        @GetMapping
        public ResponseEntity<PostResponse> getAllCategories() {
                try {
                        List<Category> categories = categoryService.getAllCategories();

                        // Convert categories to only include names
                        List<String> formattedCategories = categories.stream()
                                        .map(Category::getName)
                                        .toList();

                        return ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(new PostResponse(
                                                        true,
                                                        "Categories retrieved successfully",
                                                        formattedCategories));
                } catch (Exception e) {
                        log.error("Error retrieving categories: {}", e.getMessage());
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new PostResponse(
                                                        false,
                                                        "Error retrieving categories: " + e.getMessage(),
                                                        null));
                }
        }

        @PostMapping("/create")
        public ResponseEntity<PostResponse> createCategory(
                        @RequestParam String categoryName) {
                Category createdCategory;

                try {
                        createdCategory = categoryService.createCategory(categoryName);
                } catch (Exception e) {
                        log.error(e.getMessage());
                        return ResponseEntity.badRequest().body(new PostResponse(
                                        false, "Category creation failed", e.getMessage()));
                }

                return ResponseEntity.ok(new PostResponse(
                                true, "Category created", createdCategory));
        }

        @PutMapping("/rename")
        public ResponseEntity<PostResponse> updateCategory(
                        @RequestParam String oldCategoryName,
                        @RequestParam String newCategoryName) {
                Category updatedCategory;

                try {
                        updatedCategory = categoryService.updateCategoryName(oldCategoryName, newCategoryName);
                } catch (Exception e) {
                        log.error(e.getMessage());
                        return ResponseEntity.badRequest().body(new PostResponse(
                                        false, "Category rename failed", e.getMessage()));
                }

                return ResponseEntity.ok(new PostResponse(
                                true, "Category renamed successfully", updatedCategory));
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
