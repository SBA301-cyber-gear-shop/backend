package com.project.cybershop.pcbuild.internal.dto.admin;

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
public class BudgetProfilePurposeDto {
    private String purpose;
    private String label;
    private String description;
    private BigDecimal minBudget;
    private BigDecimal totalSharePct; // Tổng % các slot
    private List<SlotShareItem> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotShareItem {
        private Integer profileId;
        private Integer slotId;
        private Integer categoryId;
        private String categoryName;
        private Integer stepOrder;
        private BigDecimal sharePct;
    }
}

