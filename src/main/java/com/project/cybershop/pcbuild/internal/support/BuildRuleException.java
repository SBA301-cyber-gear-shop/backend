package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ của PC Build mang mã ổn định và HTTP status riêng (422 BUILD_INCOMPATIBLE, 429 RATE_LIMITED…). */
public class BuildRuleException extends BusinessException {

    private final String code;
    private final HttpStatus httpStatus;
    private final transient Object data;
    private final Long retryAfterSeconds;

    public BuildRuleException(String code, HttpStatus httpStatus, String message, Object data) {
        this(code, httpStatus, message, data, null);
    }

    /** {@code retryAfterSeconds} khác null thì response có header Retry-After. */
    public BuildRuleException(String code, HttpStatus httpStatus, String message, Object data, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.data = data;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Object getData() {
        return data;
    }
}
