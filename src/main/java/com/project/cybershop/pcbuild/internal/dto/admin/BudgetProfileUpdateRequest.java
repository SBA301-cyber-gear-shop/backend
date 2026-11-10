package com.project.cybershop.pcbuild.internal.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetProfileUpdateRequest {
    @NotNull(message = "Ngân sách tối thiểu không được để trống")
    @DecimalMin(value = "0.01", message = "Ngân sách tối thiểu phải > 0")
    private BigDecimal minBudget;

    // slotId -> sharePct (tổng phải bằng 100%)
    @NotNull(message = "Danh sách phân bổ tỷ lệ không được để trống")
    private Map<Integer, BigDecimal> slotShares;
}

