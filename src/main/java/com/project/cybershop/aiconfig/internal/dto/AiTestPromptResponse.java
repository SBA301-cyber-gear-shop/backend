package com.project.cybershop.aiconfig.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTestPromptResponse {
    private String output;
    private long latencyMs;
    private int promptTokens;
    private int completionTokens;
    private boolean simulated;
}

