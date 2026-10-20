package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPcBuildSummaryResponse {
    private Integer buildId;
    private String buildName;
    private BigDecimal totalPrice;
    private Integer orderId;
    private boolean isOrdered;
    private Integer itemCount;
    private String cpuName;
    private String gpuName;
    private String mainboardName;
    private String ramSummary;
}
