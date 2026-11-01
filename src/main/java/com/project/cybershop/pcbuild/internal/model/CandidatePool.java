package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ứng viên đã lọc theo hồ sơ (DD 6.1), mỗi role sắp theo (s giảm dần, price tăng dần, variantId tăng dần).
 *
 * @param rangesByRole role → specId → {min, max} giá trị hiệu dụng trên pool, dùng để chuẩn hóa điểm (DD 6.2)
 */
public record CandidatePool(Map<String, List<ComponentCandidateDTO>> byRole,
                            Map<String, Map<Integer, int[]>> rangesByRole) {

    public CandidatePool {
        byRole = byRole.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        rangesByRole = Map.copyOf(rangesByRole);
    }

    public List<ComponentCandidateDTO> of(String role) {
        return byRole.getOrDefault(role, List.of());
    }

    /** {min, max} của thông số trên pool của role; null nếu không ứng viên nào có giá trị. */
    public int[] range(String role, int specId) {
        Map<Integer, int[]> ranges = rangesByRole.get(role);
        return ranges != null ? ranges.get(specId) : null;
    }

    public int size() {
        return byRole.values().stream().mapToInt(List::size).sum();
    }
}
