package com.safetypin.post.exception;

public class InvalidPostDataException extends PostException {
    public InvalidPostDataException(String message) {
        super(message, "INVALID_POST_DATA");
    }
}
