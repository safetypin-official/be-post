package com.safetypin.post.exception;

public class CategoryException extends PostException {
    public CategoryException(String message) {
        super(message, "CATEGORY_ERROR");
    }
}
