package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.pcbuild.api.dto.response.BudgetAllocationItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Kế hoạch phân bổ ngân sách theo cấu hình purpose; allocation đã xếp theo step_order nên cũng là thứ tự chọn. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildPlan {
    private List<BudgetAllocationItem> allocation;
}

