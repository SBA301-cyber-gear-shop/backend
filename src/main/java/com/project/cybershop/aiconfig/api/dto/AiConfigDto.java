package com.project.cybershop.aiconfig.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDto {
    private Integer id;
    private String configKey;
    private String configName;
    private String provider;
    private String modelName;
    private String apiKey;
    private String systemPrompt;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}

