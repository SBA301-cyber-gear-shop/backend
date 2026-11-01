package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * AI lỗi hoặc hết thời gian (DD 8.5): 503 AI_UNAVAILABLE kèm Retry-After, FE cho khách thử lại.
 * Không có hồ sơ hay lời giải thích dự phòng [ĐÃ CHỐT].
 */
public class AiUnavailableException extends BusinessException {

    public enum Detail { TIMEOUT, PROVIDER_ERROR, INVALID_RESPONSE, DEADLINE_EXCEEDED, CIRCUIT_OPEN }

    public static final String MESSAGE = "Trợ lý AI đang bận hoặc phản hồi chậm. Vui lòng thử lại sau ít phút.";
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 30;

    private final Detail detail;
    private final long retryAfterSeconds;

    public AiUnavailableException(Detail detail) {
        this(detail, DEFAULT_RETRY_AFTER_SECONDS);
    }

    public AiUnavailableException(Detail detail, long retryAfterSeconds) {
        super(MESSAGE);
        this.detail = detail;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public Detail detail() {
        return detail;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    @Override
    public String getCode() {
        return "AI_UNAVAILABLE";
    }

    @Override
    public Object getData() {
        return Map.of("detail", detail.name());
    }

    @Override
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
