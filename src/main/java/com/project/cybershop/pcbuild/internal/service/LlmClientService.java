package com.project.cybershop.pcbuild.internal.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public interface LlmClientService {

    /** Lời giải thích AI viết cho bộ máy solver đã chọn (DD 8.4). AI không chọn hay đổi linh kiện. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ExplanationDraft {
        private String buildName;
        private String aiReasoning;
        private String performanceHighlight;
        private List<PartReason> partReasons = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PartReason {
            private Integer variantId;
            private String reason;
        }
    }

    /**
     * Trích hồ sơ yêu cầu (DD 8.3). Timeout mỗi lần gọi = min(profile-timeout-ms, thời gian còn lại tới deadline).
     *
     * @param deadline mốc chung của cả request /ai-suggest (bắt đầu + request-deadline-ms)
     * @throws com.project.cybershop.pcbuild.internal.support.AiUnavailableException khi hết lần thử, hết deadline hoặc mạch ngắt
     */
    RequirementProfileDto extractProfile(String systemPrompt, String userJson, Instant deadline);

    /** Viết lời giải thích (DD 8.4). Timeout mỗi lần gọi = min(timeout-ms, thời gian còn lại tới deadline). */
    ExplanationDraft explainBuild(String systemPrompt, String userJson, Instant deadline);
}
