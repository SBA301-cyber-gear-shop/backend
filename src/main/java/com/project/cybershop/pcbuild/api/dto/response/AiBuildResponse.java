package com.project.cybershop.pcbuild.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Response POST /ai-suggest (DD 9.1). Các trường cũ giữ nguyên tên và nghĩa; từ giai đoạn 2c chỉ thêm trường. */
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

    @JsonProperty("isCompatible")
    private boolean isCompatible;

    private List<CompatibilityCheckResponse> checks;
    private List<SelectedPartResponse> items;

    // Thêm ở giai đoạn 2c
    private String reasonCode;              // null | NEEDS_SATURATED | CATALOG_CEILING | REQUIREMENT_RELAXED | MAX_PRICE_REACHED
    private BudgetFitResponse budgetFit;
    private RequirementProfileDto profile;  // hồ sơ đã dùng, có label và satisfied cho từng mục
    private List<String> relaxedItems;
    private List<String> warnings;
    private CapacityResponse capacity;
    private Integer requiredPsuW;
    private BigDecimal floorPrice;          // giá bộ rẻ nhất thỏa toàn bộ hồ sơ gốc; có thể > budget
    private AiBuildMetadataResponse metadata;
}
