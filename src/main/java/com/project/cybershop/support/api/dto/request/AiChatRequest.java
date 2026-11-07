package com.project.cybershop.support.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String message;
    private String conversationId;
    @Builder.Default
    private String mode = "ai"; // "ai" or "staff"
    @Builder.Default
    private List<HistoryItem> history = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String sender; // "user" or "bot"
        private String text;
    }
}
