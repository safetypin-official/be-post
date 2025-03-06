package com.safetypin.post.exception;

public class PostException extends RuntimeException {
    private final String errorCode;

    public PostException(String message) {
        super(message);
        this.errorCode = "POST_ERROR";
    }

    public PostException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PostException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "POST_ERROR";
    }

    public PostException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
