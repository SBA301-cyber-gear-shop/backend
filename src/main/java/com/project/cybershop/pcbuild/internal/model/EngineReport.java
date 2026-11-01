package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.pcbuild.api.dto.response.CapacityResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Kết quả đánh giá của CompatibilityEngine (DD 7.1). */
public record EngineReport(
        boolean compatible,      // = không có check fail
        boolean hasWarnings,
        boolean hasUnknown,
        List<CompatibilityCheckResponse> checks,
        int requiredPsuW,
        CapacityResponse capacity,
        BigDecimal total
) {

    private static final Map<String, Integer> STATUS_RANK = Map.of("fail", 0, "warning", 1, "unknown", 2, "pass", 3);

    /** Thứ tự DD 7.5: fail, warning, unknown, pass; trong mỗi nhóm theo key. */
    public static final Comparator<CompatibilityCheckResponse> CHECK_ORDER = Comparator
            .comparing((CompatibilityCheckResponse check) -> STATUS_RANK.getOrDefault(check.getStatus(), 4))
            .thenComparing(check -> check.getKey() != null ? check.getKey() : "");

    public EngineReport {
        checks = checks.stream().sorted(CHECK_ORDER).toList();
    }

    public static EngineReport of(List<CompatibilityCheckResponse> checks, int requiredPsuW,
                                  CapacityResponse capacity, BigDecimal total) {
        return new EngineReport(
                checks.stream().noneMatch(check -> "fail".equals(check.getStatus())),
                checks.stream().anyMatch(check -> "warning".equals(check.getStatus())),
                checks.stream().anyMatch(check -> "unknown".equals(check.getStatus())),
                checks, requiredPsuW, capacity, total);
    }

    /** Thêm check ngoài engine (ví dụ STOCK của /validate) và tính lại các cờ. */
    public EngineReport withExtraChecks(List<CompatibilityCheckResponse> extraChecks) {
        if (extraChecks.isEmpty()) return this;
        List<CompatibilityCheckResponse> merged = new ArrayList<>(checks);
        merged.addAll(extraChecks);
        return of(merged, requiredPsuW, capacity, total);
    }
}
