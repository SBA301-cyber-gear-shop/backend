package com.project.cybershop.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Ném khi vi phạm quy tắc nghiệp vụ (mặc định HTTP 400).
 * Lớp con có thể đổi HTTP status và gắn mã lỗi ổn định cùng dữ liệu kèm theo cho FE.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    /** Mã lỗi ổn định (ví dụ BUILD_INCOMPATIBLE); null = không có. */
    public String getCode() {
        return null;
    }

    /** Dữ liệu kèm lỗi (ví dụ danh sách check); null = không có. */
    public Object getData() {
        return null;
    }

    /** Số giây cho header Retry-After (ví dụ dịch vụ ngoài đang lỗi); null = không gửi header. */
    public Long getRetryAfterSeconds() {
        return null;
    }
}
