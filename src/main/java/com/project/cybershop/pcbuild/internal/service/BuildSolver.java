package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.SolverInput;
import com.project.cybershop.pcbuild.internal.model.SolverResult;

import java.math.BigDecimal;

/**
 * Chọn đúng một bộ máy tất định, tổng ≤ ngân sách, sát ngân sách nhất có thể (DD 6). Thuần bộ nhớ: không gọi DB,
 * HTTP hay LLM; mọi dữ liệu đến từ SolverInput.
 */
public interface BuildSolver {

    /** Không có lời giải thì ném BuildRuleException NO_FEASIBLE_BUILD (422) kèm floorPrice, minPriority1Price, blockingItems. */
    SolverResult solve(SolverInput input);

    /** Giá bộ hợp lệ rẻ nhất với hồ sơ rỗng (DD 6.8), tính một lần cho mỗi snapshot; null nếu kho không ráp được. */
    BigDecimal floorPrice(CompatibilitySnapshot snapshot);
}
