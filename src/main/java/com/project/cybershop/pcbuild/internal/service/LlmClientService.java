package com.project.cybershop.pcbuild.internal.service;

import java.util.Map;

/** Gọi mô hình ngôn ngữ, bắt trả đúng JSON theo schema. */
public interface LlmClientService {

    /**
     * Gửi prompt hệ thống và dữ liệu, ép mô hình trả JSON theo responseSchema rồi đọc thành type.
     * Thử lại tối đa app.ai.max-retries lần khi hết giờ, lỗi kết nối, 5xx, 429 hoặc JSON hỏng.
     *
     * @throws com.project.cybershop.pcbuild.internal.support.AiUnavailableException khi vẫn thất bại
     */
    <T> T generateJson(String step, String systemPrompt, String userJson, Map<String, Object> responseSchema, Class<T> type);
}
