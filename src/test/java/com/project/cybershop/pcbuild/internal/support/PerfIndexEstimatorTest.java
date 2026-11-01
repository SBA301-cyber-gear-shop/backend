package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerfIndexEstimator - giá trị hiệu dụng của Perf_Index_1440p (DD 6.2.1)")
class PerfIndexEstimatorTest {

    private static ComponentCandidateDTO gpu(int id, Integer index, Integer tier) {
        Map<Integer, String> specs = new HashMap<>();
        if (index != null) specs.put(47, String.valueOf(index));
        if (tier != null) specs.put(48, String.valueOf(tier));
        return part(id, "gpu", "GPU " + id, 10_000_000, specs);
    }

    // ref = {3: 28,5; 5: 42; 7: 59,3; 10: 100} giống dữ liệu thật
    private final List<ComponentCandidateDTO> catalog = List.of(
            gpu(1, 27, 3), gpu(2, 30, 3), gpu(3, 42, 5), gpu(4, 58, 7), gpu(5, 60, 7), gpu(6, 60, 7), gpu(7, 100, 10),
            gpu(11, null, 1), gpu(12, null, 7), gpu(13, null, 9), gpu(14, null, 10), gpu(15, null, 12), gpu(16, null, null));

    private PerfIndexEstimator estimator() {
        Map<Integer, ParsedSpecs> parsed = new HashMap<>();
        catalog.forEach(card -> parsed.put(card.getVariantId(), ParsedSpecs.of(card)));
        return PerfIndexEstimator.build(catalog, parsed);
    }

    private Integer effective(PerfIndexEstimator estimator, int id) {
        ComponentCandidateDTO card = catalog.stream().filter(c -> c.getVariantId() == id).findFirst().orElseThrow();
        return estimator.perfIndex(card, ParsedSpecs.of(card));
    }

    @Test
    @DisplayName("Card có số đo giữ nguyên; thiếu số đo nhưng tier có tham chiếu → trung bình ref(t) làm tròn")
    void measuredValueKeptAndReferenceUsed() {
        PerfIndexEstimator estimator = estimator();
        assertThat(effective(estimator, 1)).isEqualTo(27);
        assertThat(effective(estimator, 12)).isEqualTo(59);   // ref(7) = 59,33
        assertThat(effective(estimator, 14)).isEqualTo(100);  // RTX PRO 6000 ngang RTX 5090
    }

    @Test
    @DisplayName("Tier giữa hai tier có ref → nội suy; thấp hơn mọi tier → ref(m)·t/m; cao hơn mọi tier → ref lớn nhất")
    void interpolatesAndExtrapolates() {
        PerfIndexEstimator estimator = estimator();
        assertThat(effective(estimator, 13)).isEqualTo(86);   // tier 9 giữa 7 và 10: 59,33 + (100 − 59,33) · 2/3 = 86,4
        assertThat(effective(estimator, 11)).isEqualTo(10);   // 28,5 · 1/3 = 9,5 → 10 (GTX 1650, RTX 3050)
        assertThat(effective(estimator, 15)).isEqualTo(100);
    }

    @Test
    @DisplayName("Thiếu cả hai thông số → không có giá trị hiệu dụng")
    void missingBothHasNoValue() {
        assertThat(effective(estimator(), 16)).isNull();
    }
}
