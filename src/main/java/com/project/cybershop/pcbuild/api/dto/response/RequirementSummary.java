package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementSummary {
    private Long budget;
    private String purpose;
    private List<String> selectionOrder;
    private List<BudgetAllocationItem> budgetAllocation;
}

