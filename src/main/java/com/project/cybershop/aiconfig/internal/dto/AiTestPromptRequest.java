package com.project.cybershop.aiconfig.internal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTestPromptRequest {

    private String configKey;
    private String provider;
    private String modelName;
    private String apiKey;
    private String systemPrompt;

    @NotBlank(message = "Nội dung câu hỏi thử nghiệm không được để trống")
    private String userMessage;
}

