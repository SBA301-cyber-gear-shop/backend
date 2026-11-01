package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.BalancePairDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.support.PerfIndexEstimator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.REQUIRED_SPECS;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.RULES;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("PsuSizer và PartScorer - công thức nguồn (DD 6.5) và chấm điểm theo hồ sơ (DD 6.2, 6.3)")
class PsuSizerAndScorerTest {

    private static final Map<Integer, String> SPEC_NAMES = Map.of(2, "TDP_Power", 9, "Capacity_GB", 12, "VRAM_GB",
            14, "Wattage", 15, "Efficiency_Rating", 22, "Core_Count", 47, "Perf_Index_1440p", 48, "Perf_Tier", 50, "Rec_PSU_W");

    private static CompatibilitySnapshot snapshot(List<ComponentCandidateDTO> candidates) {
        Map<Integer, ParsedSpecs> parsed = new HashMap<>();
        candidates.forEach(candidate -> parsed.put(candidate.getVariantId(), ParsedSpecs.of(candidate)));
        PerfIndexEstimator estimator = PerfIndexEstimator.build(
                candidates.stream().filter(candidate -> "gpu".equals(candidate.getCategorySlug())).toList(), parsed);
        return new CompatibilitySnapshot(candidates, parsed, RULES, REQUIRED_SPECS, Map.of(), SPEC_NAMES, null, estimator,
                Instant.parse("2026-10-30T00:00:00Z"));
    }

    private static ValidatedProfile profile(Map<String, Double> weights, List<ProfilePreferenceDto> preferences,
                                            List<BalancePairDto> pairs) {
        RequirementProfileDto dto = RequirementProfileDto.builder().roleWeights(new LinkedHashMap<>(weights))
                .preferences(new ArrayList<>(preferences)).balancePairs(new ArrayList<>(pairs)).build();
        return new ValidatedProfile(dto, Archetype.GENERAL, List.of());
    }

    private static CandidatePool poolOf(CompatibilitySnapshot snapshot, Map<String, List<ComponentCandidateDTO>> byRole) {
        Map<String, Map<Integer, int[]>> ranges = new HashMap<>();
        byRole.forEach((role, parts) -> {
            Map<Integer, int[]> roleRanges = new HashMap<>();
            for (ComponentCandidateDTO candidate : parts) {
                for (Integer specId : List.of(9, 12, 14, 22, 47)) {
                    Integer value = snapshot.effectiveInt(candidate, specId);
                    if (value != null) roleRanges.merge(specId, new int[]{value, value},
                            (a, b) -> new int[]{Math.min(a[0], b[0]), Math.max(a[1], b[1])});
                }
            }
            ranges.put(role, roleRanges);
        });
        return new CandidatePool(byRole, ranges);
    }

    private static ProfilePreferenceDto pref(String role, String spec, String direction, double weight, Double cap, String... values) {
        return ProfilePreferenceDto.builder().role(role).spec(spec).direction(direction).weight(weight).cap(cap)
                .values(new ArrayList<>(List.of(values))).build();
    }

    private static ProfilePreferenceDto metricPref(String metric, double weight, Double cap) {
        return ProfilePreferenceDto.builder().metric(metric).direction("MAX").weight(weight).cap(cap).values(new ArrayList<>()).build();
    }

    @Nested
    @DisplayName("PsuSizer")
    class PsuSizerCases {
        private final PsuSizerImpl sizer = new PsuSizerImpl();

        @Test
        @DisplayName("1 GPU: ceil((162 + 575 + 80) × 1,3) = 1063 W (pseudo-code DD 6.5 làm tròn lên; 13.1 ghi ≈1062)")
        void oneGpuUsesThirtyPercentHeadroom() {
            ComponentCandidateDTO cpu = part(1, "cpu", "9800X3D", 12_000_000, Map.of(2, "162"));
            ComponentCandidateDTO gpu = part(2, "gpu", "RTX 5090", 180_000_000, Map.of(2, "575", 50, "1000"));
            var need = sizer.requiredW(List.of(new BuildLine("cpu", cpu, 1), new BuildLine("gpu", gpu, 1)), snapshot(List.of(cpu, gpu)));
            assertThat(need.loadW()).isEqualTo(817);
            assertThat(need.requiredW()).isEqualTo(1063);
        }

        @Test
        @DisplayName("2 GPU dùng hệ số 1,35; Rec_PSU_W lớn hơn công thức thì lấy Rec_PSU_W")
        void multiGpuAndRecommendation() {
            ComponentCandidateDTO cpu = part(1, "cpu", "CPU", 1, Map.of(2, "100"));
            ComponentCandidateDTO gpu = part(2, "gpu", "GPU", 1, Map.of(2, "200", 50, "650"));
            CompatibilitySnapshot snapshot = snapshot(List.of(cpu, gpu));
            // 2 GPU: max(ceil((100 + 400 + 80) × 1,35) = 783, 650 + 200 = 850)
            assertThat(sizer.requiredW(List.of(new BuildLine("cpu", cpu, 1), new BuildLine("gpu", gpu, 2)), snapshot).requiredW()).isEqualTo(850);
            // 1 GPU: max(ceil(380 × 1,3) = 494, 650)
            assertThat(sizer.requiredW(List.of(new BuildLine("cpu", cpu, 1), new BuildLine("gpu", gpu, 1)), snapshot).requiredW()).isEqualTo(650);
        }

        @Test
        @DisplayName("fix: nguồn rẻ nhất đủ công suất; trong biên +10% giá thì lấy nguồn điểm cao nhất")
        void fixChoosesCheapestWithinPremium() {
            ComponentCandidateDTO cpu = part(1, "cpu", "CPU", 1, Map.of(2, "100"));
            ComponentCandidateDTO weak = part(10, "psu", "450W", 600_000, Map.of(14, "450", 15, "GOLD"));
            ComponentCandidateDTO bronze = part(11, "psu", "550W Bronze", 1_000_000, Map.of(14, "550", 15, "BRONZE"));
            ComponentCandidateDTO gold = part(12, "psu", "550W Gold", 1_090_000, Map.of(14, "550", 15, "GOLD"));
            ComponentCandidateDTO expensive = part(13, "psu", "650W Gold", 2_000_000, Map.of(14, "650", 15, "GOLD"));
            CompatibilitySnapshot snapshot = snapshot(List.of(cpu, weak, bronze, gold, expensive));
            CandidatePool pool = poolOf(snapshot, Map.of("psu", List.of(weak, bronze, gold, expensive)));
            ScoreContext ctx = ScoreContext.of(profile(Map.of("psu", 1.0),
                    List.of(pref("psu", "Efficiency_Rating", "PREFER", 1.0, null, "GOLD")), List.of()), pool, snapshot, null, role -> List.of());

            // cần ceil(180 × 1,3) = 234 W → nguồn 450W rẻ nhất đủ; nhưng thêm GPU để cần 520 W
            ComponentCandidateDTO gpu = part(2, "gpu", "GPU", 1, Map.of(2, "220", 50, "500"));
            List<BuildLine> fixed = sizer.fix(List.of(new BuildLine("cpu", cpu, 1), new BuildLine("gpu", gpu, 1)), ctx);

            assertThat(fixed).anyMatch(line -> "psu".equals(line.role()) && line.part().getVariantId() == 12);
        }
    }

    @Nested
    @DisplayName("PartScorer")
    class ScorerCases {
        private final PartScorerImpl scorer = new PartScorerImpl();

        @Test
        @DisplayName("MAX có cap: vượt cap không tăng điểm")
        void capSaturates() {
            ComponentCandidateDTO eight = part(1, "cpu", "8 nhân", 1, Map.of(22, "8"));
            ComponentCandidateDTO sixteen = part(2, "cpu", "16 nhân", 1, Map.of(22, "16"));
            ComponentCandidateDTO four = part(3, "cpu", "4 nhân", 1, Map.of(22, "4"));
            CompatibilitySnapshot snapshot = snapshot(List.of(eight, sixteen, four));
            ScoreContext ctx = ScoreContext.of(profile(Map.of("cpu", 1.0), List.of(pref("cpu", "Core_Count", "MAX", 1.0, 8.0)), List.of()),
                    poolOf(snapshot, Map.of("cpu", List.of(eight, sixteen, four))), snapshot, null, role -> List.of());

            assertThat(scorer.partScore("cpu", eight, ctx)).isEqualTo(1.0);
            assertThat(scorer.partScore("cpu", sixteen, ctx)).isEqualTo(1.0);
            assertThat(scorer.partScore("cpu", four, ctx)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Metric TOTAL_RAM_GB tính theo quantity; nhiều SSD lấy trung bình theo quantity")
        void metricsUseQuantity() {
            ComponentCandidateDTO ram = part(1, "ram", "16GB", 1, Map.of(9, "16"));
            ComponentCandidateDTO small = part(2, "ssd", "SSD A", 1, Map.of(9, "500", 14, "100"));
            ComponentCandidateDTO large = part(3, "ssd", "SSD B", 1, Map.of(9, "2000", 14, "300"));
            CompatibilitySnapshot snapshot = snapshot(List.of(ram, small, large));
            ScoreContext ctx = ScoreContext.of(profile(Map.of("ram", 0.5, "ssd", 0.5),
                    List.of(metricPref("TOTAL_RAM_GB", 1.0, 64.0), pref("ssd", "Wattage", "MAX", 1.0, null)), List.of()),
                    poolOf(snapshot, Map.of("ram", List.of(ram), "ssd", List.of(small, large))), snapshot, null, role -> List.of());

            assertThat(scorer.roleScore("ram", List.of(new BuildLine("ram", ram, 2)), ctx)).isCloseTo(0.5, within(1e-9));
            assertThat(scorer.roleScore("ram", List.of(new BuildLine("ram", ram, 4)), ctx)).isCloseTo(1.0, within(1e-9));
            // (0 × 3 + 1 × 1) / 4 = 0,25
            assertThat(scorer.roleScore("ssd", List.of(new BuildLine("ssd", small, 3), new BuildLine("ssd", large, 1)), ctx))
                    .isCloseTo(0.25, within(1e-9));
        }

        @Test
        @DisplayName("PREFER đúng/sai; không có preference → dùng ưu tiên mặc định; không có GPU → 0, không có tản → 0,2")
        void preferAndDefaults() {
            ComponentCandidateDTO gold = part(1, "psu", "Gold", 1, Map.of(15, "GOLD"));
            ComponentCandidateDTO bronze = part(2, "psu", "Bronze", 1, Map.of(15, "BRONZE"));
            CompatibilitySnapshot snapshot = snapshot(List.of(gold, bronze));
            ScoreContext ctx = ScoreContext.of(profile(Map.of("psu", 1.0), List.of(), List.of()),
                    poolOf(snapshot, Map.of("psu", List.of(gold, bronze))), snapshot, null,
                    role -> "psu".equals(role) ? List.of(pref("psu", "Efficiency_Rating", "PREFER", 1.0, null, "GOLD")) : List.of());

            assertThat(scorer.partScore("psu", gold, ctx)).isEqualTo(1.0);
            assertThat(scorer.partScore("psu", bronze, ctx)).isEqualTo(0.0);
            assertThat(scorer.roleScore("gpu", List.of(), ctx)).isEqualTo(0.0);
            assertThat(scorer.roleScore("cooler", List.of(), ctx)).isEqualTo(0.2);
        }

        @Test
        @DisplayName("balancePairs trừ 0,15 × (|s(a) − s(b)| − maxGap) khi lệch quá maxGap")
        void balancePenalty() {
            ComponentCandidateDTO strongCpu = part(1, "cpu", "CPU mạnh", 1, Map.of(22, "16"));
            ComponentCandidateDTO weakCpu = part(2, "cpu", "CPU yếu", 1, Map.of(22, "4"));
            ComponentCandidateDTO gpu = part(3, "gpu", "GPU", 1, Map.of(47, "10"));
            ComponentCandidateDTO topGpu = part(4, "gpu", "GPU mạnh", 1, Map.of(47, "100"));
            CompatibilitySnapshot snapshot = snapshot(List.of(strongCpu, weakCpu, gpu, topGpu));
            ScoreContext ctx = ScoreContext.of(profile(Map.of("cpu", 0.5, "gpu", 0.5),
                    List.of(pref("cpu", "Core_Count", "MAX", 1.0, null), pref("gpu", "Perf_Index_1440p", "MAX", 1.0, null)),
                    List.of(new BalancePairDto("cpu", "gpu", 0.2))),
                    poolOf(snapshot, Map.of("cpu", List.of(strongCpu, weakCpu), "gpu", List.of(gpu, topGpu))), snapshot, null, role -> List.of());

            // s(cpu) = 1, s(gpu) = 0 → U = 0,5 − 0,15 × 0,8 = 0,38
            double utility = scorer.utility(List.of(new BuildLine("cpu", strongCpu, 1), new BuildLine("gpu", gpu, 1)), ctx);
            assertThat(utility).isCloseTo(0.38, within(1e-9));
        }

        @Test
        @DisplayName("GPU thiếu Perf_Index dùng giá trị hiệu dụng: RTX PRO 6000 (tier 10) ngang RTX 5090, cao hơn GTX 1650")
        void effectivePerfIndexInScoring() {
            ComponentCandidateDTO rtx5090 = part(1, "gpu", "RTX 5090", 1, Map.of(47, "100", 48, "10"));
            ComponentCandidateDTO rtx5080 = part(2, "gpu", "RTX 5080", 1, Map.of(47, "77", 48, "9"));
            ComponentCandidateDTO rx9060 = part(3, "gpu", "RX 9060 XT", 1, Map.of(47, "28", 48, "3"));
            ComponentCandidateDTO pro6000 = part(4, "gpu", "RTX PRO 6000", 1, Map.of(48, "10"));
            ComponentCandidateDTO gtx1650 = part(5, "gpu", "GTX 1650", 1, Map.of(48, "1"));
            List<ComponentCandidateDTO> gpus = List.of(rtx5090, rtx5080, rx9060, pro6000, gtx1650);
            CompatibilitySnapshot snapshot = snapshot(gpus);
            ScoreContext ctx = ScoreContext.of(profile(Map.of("gpu", 1.0), List.of(pref("gpu", "Perf_Index_1440p", "MAX", 1.0, null)), List.of()),
                    poolOf(snapshot, Map.of("gpu", gpus)), snapshot, BigDecimal.ONE, role -> List.of());

            assertThat(scorer.partScore("gpu", pro6000, ctx)).isEqualTo(scorer.partScore("gpu", rtx5090, ctx));
            assertThat(scorer.partScore("gpu", pro6000, ctx)).isGreaterThan(scorer.partScore("gpu", gtx1650, ctx));
        }
    }
}
