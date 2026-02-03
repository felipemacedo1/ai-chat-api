package com.chatai.exception;

/**
 * Exception thrown when AI provider operations fail.
 * This includes network errors, API errors, rate limiting, and configuration issues.
 */
public class AiServiceException extends RuntimeException {
    
    private final boolean retryable;
    private final String errorCode;
    
    public AiServiceException(String message) {
        super(message);
        this.retryable = true;
        this.errorCode = "AI_ERROR";
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
        this.errorCode = "AI_ERROR";
    }
    
    public AiServiceException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
        this.errorCode = "AI_ERROR";
    }
    
    public AiServiceException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
        this.errorCode = "AI_ERROR";
    }
    
    public AiServiceException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    public AiServiceException(String message, Throwable cause, String errorCode, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
    
    /**
     * Indicates whether the operation can be retried.
     * @return true if the operation is retryable, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * Gets the error code for this exception.
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
