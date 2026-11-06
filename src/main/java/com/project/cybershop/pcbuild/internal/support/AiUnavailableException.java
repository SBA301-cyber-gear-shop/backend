package com.project.cybershop.pcbuild.internal.support;

import org.springframework.http.HttpStatus;

/** Gọi AI thất bại sau khi đã thử lại (hết giờ, lỗi nhà cung cấp, JSON hỏng, chưa có API key). */
public class AiUnavailableException extends PcBuildException {

    public AiUnavailableException(String reason) {
        super("AI_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Trợ lý AI tạm thời không phản hồi: " + reason);
    }
}
