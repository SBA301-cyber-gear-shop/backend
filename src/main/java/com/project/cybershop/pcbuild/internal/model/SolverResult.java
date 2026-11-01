package com.project.cybershop.pcbuild.internal.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kết quả BuildSolver (DD 6.1): đúng một bộ máy, tổng luôn ≤ ngân sách.
 *
 * @param reasonCode null | NEEDS_SATURATED | CATALOG_CEILING | REQUIREMENT_RELAXED
 * @param profile    hồ sơ sau khi hạ (mục đã hạ có giá trị mới hoặc đã bị bỏ)
 * @param floorPrice giá bộ hợp lệ rẻ nhất thỏa toàn bộ hồ sơ GỐC, ghi một lần trước mọi bước hạ; có thể > budget; null nếu không có
 */
public record SolverResult(
        List<BuildLine> lines,
        BigDecimal total,
        BigDecimal leftover,
        double utility,
        EngineReport compatibility,
        String reasonCode,
        List<String> relaxedItems,
        BigDecimal floorPrice,
        int iterations,
        List<String> warnings,
        ValidatedProfile profile
) {
    public SolverResult {
        lines = List.copyOf(lines);
        relaxedItems = List.copyOf(relaxedItems);
        warnings = List.copyOf(warnings);
    }
}
