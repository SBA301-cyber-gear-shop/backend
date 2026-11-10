package com.project.cybershop.aiconfig.api;

import com.project.cybershop.aiconfig.api.dto.AiConfigDto;

import java.util.Optional;

/**
 * Public API của module aiconfig dành cho các module khác (pcbuild, support...) truy xuất cấu hình AI đang hoạt động.
 */
public interface AiConfigPublicApi {

    /**
     * Tìm cấu hình đang kích hoạt (is_active = true) theo configKey.
     */
    Optional<AiConfigDto> findActiveConfig(String configKey);

    /**
     * Lấy cấu hình đang kích hoạt (is_active = true) theo configKey, trả về null nếu chưa có.
     */
    AiConfigDto getActiveConfig(String configKey);

    /**
     * Lấy System Prompt đang kích hoạt, nếu rỗng hoặc null thì trả về defaultPrompt.
     */
    String getActivePrompt(String configKey, String defaultPrompt);
}

