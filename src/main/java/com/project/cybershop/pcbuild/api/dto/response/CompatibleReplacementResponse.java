package com.project.cybershop.pcbuild.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibleReplacementResponse {
    private Integer variantId;
    private String productName;
    private String variantName;
    private BigDecimal price;
    private String imageUrl;
    private Integer stockQuantity;
    private Map<String, String> specs;

    @JsonProperty("isCompatible")
    private boolean isCompatible;

    private String compatibilityNote;

    // Thêm ở giai đoạn 1 (DD 9.3): kết quả engine trên bộ máy sau khi thay
    private String status;              // pass, warning, unknown, fail
    private List<String> failedChecks;
    private List<String> warningChecks;
    private BigDecimal priceDelta;      // newTotal - tổng hiện tại
    private BigDecimal newTotal;
    private BigDecimal newPercent;      // newTotal / budget × 100, null khi không có budget
}
