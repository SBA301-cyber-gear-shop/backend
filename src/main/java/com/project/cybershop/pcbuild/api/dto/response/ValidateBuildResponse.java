package com.project.cybershop.pcbuild.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Response của POST /api/v1/pc-build/validate (DD 9.2). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateBuildResponse {
    @JsonProperty("isCompatible")
    private boolean isCompatible;

    @JsonProperty("hasWarnings")
    private boolean hasWarnings;

    @JsonProperty("hasUnknown")
    private boolean hasUnknown;

    private BigDecimal totalPrice;
    private BudgetFitResponse budgetFit;   // null khi request không có budget
    private List<CompatibilityCheckResponse> checks;
    private int requiredPsuW;
    private CapacityResponse capacity;
    private List<SelectedPartResponse> items;
}
