package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ của module pcbuild: có mã ổn định, HTTP status, dữ liệu kèm theo và Retry-After cho FE. */
public class PcBuildException extends BusinessException {

    private final String code;
    private final HttpStatus httpStatus;
    private final Object data;
    private final Long retryAfterSeconds;

    public PcBuildException(String code, HttpStatus httpStatus, String message) {
        this(code, httpStatus, message, null, null);
    }

    public PcBuildException(String code, HttpStatus httpStatus, String message, Object data, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.data = data;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
