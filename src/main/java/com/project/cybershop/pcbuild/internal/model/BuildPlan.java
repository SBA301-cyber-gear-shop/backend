package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.pcbuild.api.dto.response.BudgetAllocationItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Kế hoạch phân bổ ngân sách và thứ tự chọn linh kiện theo cấu hình purpose. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildPlan {
    private List<String> selectionOrder;
    private List<BudgetAllocationItem> allocation;
}

