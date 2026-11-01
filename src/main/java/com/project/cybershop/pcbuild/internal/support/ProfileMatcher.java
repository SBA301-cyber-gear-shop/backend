package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kiểm một linh kiện có thỏa constraint, một build có thỏa metric của hồ sơ.
 * Dùng chung cho lọc pool (DD 6.1), check P:* (7.4) và cờ satisfied trả cho khách, để không có hai kết quả khác nhau.
 */
public final class ProfileMatcher {

    /** Thông số ảo trong từ điển: hãng không nằm trong specification_values mà là brands.name của sản phẩm. */
    public static final String BRAND_SPEC = "Brand";

    private static final Set<String> NUMERIC_OPS = Set.of("GTE", "LTE", "EQ");

    private ProfileMatcher() {
    }

    /** Ứng viên thiếu giá trị của thông số bị coi là không thỏa (riêng Perf_Index_1440p dùng giá trị hiệu dụng). */
    public static boolean matches(ProfileConstraintDto constraint, ComponentCandidateDTO part, CompatibilitySnapshot snapshot) {
        if (BRAND_SPEC.equals(constraint.getSpec())) {
            if (part.getBrandName() == null) return false;
            boolean listed = upper(constraint.getValues()).contains(part.getBrandName().toUpperCase(Locale.ROOT));
            return "NOT_IN".equals(constraint.getOp()) ? !listed : "IN".equals(constraint.getOp()) && listed;
        }
        Integer specId = snapshot.specId(constraint.getSpec());
        if (specId == null || constraint.getOp() == null) return false;
        String op = constraint.getOp();
        if (NUMERIC_OPS.contains(op)) {
            Integer value = snapshot.effectiveInt(part, specId);
            return value != null && constraint.getValue() != null && compare(op, value, constraint.getValue());
        }
        ParsedSpecs specs = snapshot.parsed(part);
        String raw = specs.raw().get(specId);
        if (raw == null) return false;
        Set<String> wanted = upper(constraint.getValues());
        switch (op) {
            case "IN":
                return wanted.contains(raw.toUpperCase(Locale.ROOT));
            case "NOT_IN":
                return !wanted.contains(raw.toUpperCase(Locale.ROOT));
            case "CONTAINS":
                return specs.lists().getOrDefault(specId, Set.of()).containsAll(wanted);
            default:
                return false;
        }
    }

    public static boolean matches(ProfileMetricDto metric, Map<MetricKind, Integer> values) {
        MetricKind kind = MetricKind.parse(metric.getMetric()).orElse(null);
        if (kind == null || metric.getValue() == null || metric.getOp() == null) return false;
        return compare(metric.getOp(), values.getOrDefault(kind, 0), metric.getValue());
    }

    public static boolean compare(String op, double actual, double expected) {
        switch (op) {
            case "GTE":
                return actual >= expected;
            case "LTE":
                return actual <= expected;
            case "EQ":
                return actual == expected;
            default:
                return false;
        }
    }

    private static Set<String> upper(List<String> values) {
        return values == null ? Set.of()
                : values.stream().map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    }
}
