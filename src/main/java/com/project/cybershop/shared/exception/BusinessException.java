package com.project.cybershop.shared.exception;

/**
 * Ném khi vi phạm quy tắc nghiệp vụ (HTTP 400).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
