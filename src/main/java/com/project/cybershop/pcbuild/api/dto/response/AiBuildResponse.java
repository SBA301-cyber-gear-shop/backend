package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBuildResponse {
    private Integer buildId;
    private String buildName;
    private String purpose;
    private BigDecimal totalPrice;
    private BigDecimal budget;
    private String aiReasoning;
    private String performanceHighlight;

    @com.fasterxml.jackson.annotation.JsonProperty("isCompatible")
    private boolean isCompatible;

    private List<CompatibilityCheckResponse> checks;
    private List<SelectedPartResponse> items;
}
