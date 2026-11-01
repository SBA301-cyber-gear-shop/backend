package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Giá trị hiệu dụng của Perf_Index_1440p (DD 6.2.1): card có số đo giữ nguyên; card chỉ có Perf_Tier được ước lượng
 * từ bảng tham chiếu ref(tier) = trung bình Perf_Index của các card có cả hai thông số. Dựng một lần cùng snapshot.
 * Giá trị ước lượng chỉ dùng trong bộ nhớ (chấm điểm, lọc ràng buộc, từ điển), không ghi DB và không hiện cho khách.
 */
@Slf4j
public final class PerfIndexEstimator {

    public static final int PERF_INDEX_SPEC_ID = 47;
    public static final int PERF_TIER_SPEC_ID = 48;

    private final TreeMap<Integer, Double> referenceByTier;
    private final Map<Integer, Integer> effectiveByVariantId;

    private PerfIndexEstimator(TreeMap<Integer, Double> referenceByTier, Map<Integer, Integer> effectiveByVariantId) {
        this.referenceByTier = referenceByTier;
        this.effectiveByVariantId = Map.copyOf(effectiveByVariantId);
    }

    /** @param gpus ứng viên GPU kèm spec đã phân tích */
    public static PerfIndexEstimator build(List<ComponentCandidateDTO> gpus, Map<Integer, ParsedSpecs> parsedByVariantId) {
        Map<Integer, List<Integer>> indexesByTier = new HashMap<>();
        for (ComponentCandidateDTO gpu : gpus) {
            ParsedSpecs specs = parsedByVariantId.getOrDefault(gpu.getVariantId(), ParsedSpecs.of(gpu));
            Integer index = specs.ints().get(PERF_INDEX_SPEC_ID);
            Integer tier = specs.ints().get(PERF_TIER_SPEC_ID);
            if (index != null && tier != null) {
                indexesByTier.computeIfAbsent(tier, key -> new ArrayList<>()).add(index);
            }
        }
        TreeMap<Integer, Double> reference = new TreeMap<>();
        indexesByTier.forEach((tier, indexes) ->
                reference.put(tier, indexes.stream().mapToInt(Integer::intValue).average().orElse(0)));

        PerfIndexEstimator estimator = new PerfIndexEstimator(reference, Map.of());
        Map<Integer, Integer> effective = new HashMap<>();
        for (ComponentCandidateDTO gpu : gpus) {
            Integer value = estimator.estimate(parsedByVariantId.getOrDefault(gpu.getVariantId(), ParsedSpecs.of(gpu)));
            if (value != null) {
                effective.put(gpu.getVariantId(), value);
            } else {
                log.warn("GPU variantId={} thiếu cả Perf_Index_1440p và Perf_Tier, không có giá trị hiệu năng", gpu.getVariantId());
            }
        }
        return new PerfIndexEstimator(reference, effective);
    }

    /** Giá trị hiệu dụng của một GPU; null nếu thiếu cả Perf_Index_1440p và Perf_Tier. */
    public Integer perfIndex(ComponentCandidateDTO gpu, ParsedSpecs specs) {
        Integer cached = gpu.getVariantId() != null ? effectiveByVariantId.get(gpu.getVariantId()) : null;
        return cached != null ? cached : estimate(specs);
    }

    public Map<Integer, Double> referenceByTier() {
        return Map.copyOf(referenceByTier);
    }

    private Integer estimate(ParsedSpecs specs) {
        Integer measured = specs.ints().get(PERF_INDEX_SPEC_ID);
        if (measured != null) return measured;
        Integer tier = specs.ints().get(PERF_TIER_SPEC_ID);
        if (tier == null || referenceByTier.isEmpty()) return null;

        Double exact = referenceByTier.get(tier);
        if (exact != null) return (int) Math.round(exact);
        Map.Entry<Integer, Double> lower = referenceByTier.lowerEntry(tier);
        Map.Entry<Integer, Double> higher = referenceByTier.higherEntry(tier);
        if (lower == null) {
            // Thấp hơn mọi tier có tham chiếu: tỉ lệ qua gốc 0
            return (int) Math.round(higher.getValue() * tier / higher.getKey());
        }
        if (higher == null) {
            return (int) Math.round(lower.getValue());
        }
        double fraction = (double) (tier - lower.getKey()) / (higher.getKey() - lower.getKey());
        return (int) Math.round(lower.getValue() + (higher.getValue() - lower.getValue()) * fraction);
    }
}
