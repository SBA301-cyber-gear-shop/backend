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
public class SavedPcBuildDetailResponse {
    private Integer buildId;
    private String buildName;
    private BigDecimal totalPrice;
    private Integer orderId;
    private boolean isOrdered;
    private List<SelectedPartResponse> items;
}
