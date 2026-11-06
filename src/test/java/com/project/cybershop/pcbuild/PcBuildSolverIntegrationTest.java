package com.project.cybershop.pcbuild;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;
import com.project.cybershop.pcbuild.internal.model.SolverInput;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.PartScorer;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.PsuSizer;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import com.project.cybershop.pcbuild.internal.service.impl.GreedyBuildSolver;
import com.project.cybershop.pcbuild.internal.support.BuildMetrics;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.pcbuild.internal.support.CandidatePoolBuilder;
import com.project.cybershop.pcbuild.internal.support.ProfileMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Solver trên dữ liệu thật biến thể 1–118 (DD 13.3): golden test so với bộ mẫu chuyên gia và quét ngân sách.
 * Cấu hình @SpringBootTest giống hệt PcBuildEngineIntegrationTest để dùng chung context và DB H2 đã nạp seed.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pcbuild;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:purchase-schema.sql,classpath:pcbuild/pcbuild-schema.sql",
        "spring.sql.init.data-locations=classpath:pcbuild-seed.sql"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@DisplayName("BuildSolver - golden test và quét ngân sách trên dữ liệu thật (biến thể 1–118)")
class PcBuildSolverIntegrationTest {

    private static final List<Long> SWEEP_BUDGETS = List.of(8_000_000L, 8_000_001L, 10_000_000L, 12_000_000L, 15_000_000L,
            20_000_000L, 25_000_000L, 30_000_000L, 40_000_000L, 50_000_000L, 60_000_000L, 80_000_000L, 100_000_000L,
            120_000_000L, 150_000_000L, 200_000_000L, 250_000_000L, 300_000_000L, 400_000_000L, 500_000_000L,
            700_000_000L, 1_000_000_000L, 1_500_000_000L, 3_000_000_000L, 9_999_999_999L);

    @Autowired
    private CandidateRepositoryFacade facade;

    @Autowired
    private BuildSolver solver;

    @Autowired
    private ProfilePresets presets;

    @Autowired
    private ProfileValidator validator;

    @Autowired
    private PartScorer partScorer;

    @Autowired
    private CompatibilityEngine compatibilityEngine;

    @Autowired
    private PsuSizer psuSizer;

    @Autowired
    private CandidatePoolBuilder candidatePoolBuilder;

    private ValidatedProfile fixture(CompatibilitySnapshot snapshot, Archetype archetype, BigDecimal budget) {
        ValidatedProfile profile = validator.validate(presets.preset(archetype, BuildTier.of(budget)), snapshot.dictionary(),
                ProfileValidator.SOURCE_AI, null);
        assertThat(profile.profile().getDroppedItems()).as("hồ sơ mẫu %s phải qua validator", archetype).isEmpty();
        return profile;
    }

    private SolverResult solve(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget) {
        CandidatePool pool = facade.buildPool(snapshot, profile, budget);
        return solver.solve(new SolverInput(budget, profile, pool, snapshot));
    }

    @Test
    @DisplayName("Khi điểm bão hòa, solver thử cấu hình đắt hơn mà không nhồi thêm linh kiện")
    void budgetFallbackImprovesPriceWithoutAddingParts() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        ValidatedProfile profile = fixture(snapshot, Archetype.GENERAL, budget);
        PartScorer flatScorer = new PartScorer() {
            public double utility(List<BuildLine> lines, ScoreContext ctx) { return 0.5; }
            public double roleScore(String role, List<BuildLine> lines, ScoreContext ctx) {
                return partScorer.roleScore(role, lines, ctx);
            }
            public double partScore(String role, ComponentCandidateDTO part, ScoreContext ctx) {
                return partScorer.partScore(role, part, ctx);
            }
        };
        GreedyBuildSolver plateauSolver = new GreedyBuildSolver(compatibilityEngine, psuSizer, flatScorer,
                candidatePoolBuilder, presets);
        SolverResult result = plateauSolver.solve(new SolverInput(budget, profile, facade.buildPool(snapshot, profile, budget), snapshot));

        assertThat(result.floorPrice()).isNotNull();
        assertThat(result.floorPrice()).isLessThan(budget.multiply(new BigDecimal("0.90")));
        assertThat(result.total()).isGreaterThan(result.floorPrice()).isLessThanOrEqualTo(budget);
        assertThat(result.total()).isGreaterThanOrEqualTo(budget.multiply(new BigDecimal("0.90")));
        assertThat(result.compatibility().compatible()).isTrue();
        assertThat(result.lines()).allMatch(line -> line.quantity() == 1);
    }

    /** Bộ mẫu DD 13.3 (trừ base7 vì dưới minBudget): mã, hồ sơ mẫu, B gốc, variantId → số lượng. */
    static Stream<Arguments> goldenBuilds() {
        return Stream.of(
                golden("base10", Archetype.GENERAL, 10_000_000, 3, 1, 31, 1, 51, 1, 80, 1, 92, 1, 102, 1),
                golden("game15", Archetype.GAMING, 15_000_000, 17, 1, 41, 1, 51, 1, 64, 1, 80, 1, 92, 1, 103, 1, 112, 1),
                golden("game30", Archetype.GAMING, 30_000_000, 17, 1, 41, 1, 51, 2, 69, 1, 83, 1, 94, 1, 103, 1, 112, 1),
                golden("game60", Archetype.GAMING, 60_000_000, 9, 1, 36, 1, 55, 1, 72, 1, 83, 1, 95, 1, 105, 1, 113, 1),
                golden("game120", Archetype.GAMING, 120_000_000, 11, 1, 37, 1, 56, 1, 75, 1, 86, 1, 96, 1, 106, 1, 115, 1),
                golden("game250", Archetype.GAMING, 250_000_000, 10, 1, 38, 1, 57, 1, 77, 1, 86, 1, 98, 1, 109, 1, 115, 1),
                golden("game_top", Archetype.GAMING, 1_000_000_000, 11, 1, 39, 1, 59, 1, 78, 1, 90, 1, 100, 1, 110, 1, 116, 1),
                golden("gfx30", Archetype.CREATIVE, 30_000_000, 17, 1, 43, 1, 51, 2, 67, 1, 83, 1, 94, 1, 103, 1, 112, 1),
                golden("gfx60", Archetype.CREATIVE, 60_000_000, 21, 1, 43, 1, 51, 4, 70, 1, 83, 1, 82, 1, 94, 1, 105, 1, 113, 1),
                golden("gfx120", Archetype.CREATIVE, 120_000_000, 13, 1, 37, 1, 58, 1, 74, 1, 89, 1, 95, 1, 105, 1, 114, 1),
                golden("gfx250", Archetype.CREATIVE, 250_000_000, 13, 1, 37, 1, 58, 1, 77, 1, 86, 1, 98, 1, 109, 1, 114, 1),
                golden("gfx_tr600", Archetype.CREATIVE, 600_000_000, 26, 1, 48, 1, 62, 4, 77, 1, 89, 1, 99, 1, 111, 1, 117, 1),
                golden("gfx_tr1050", Archetype.CREATIVE, 1_050_000_000, 26, 1, 48, 1, 62, 8, 77, 2, 90, 1, 101, 1, 111, 1, 117, 1),
                golden("ai120", Archetype.AI_DATA, 120_000_000, 8, 1, 40, 1, 58, 1, 70, 2, 88, 1, 96, 1, 107, 1, 112, 1),
                golden("ai_pro", Archetype.AI_DATA, 1_150_000_000, 25, 1, 48, 1, 62, 4, 79, 2, 90, 1, 99, 1, 111, 1, 117, 1),
                golden("ai_epyc", Archetype.AI_DATA, 1_150_000_000, 29, 2, 49, 1, 62, 8, 79, 1, 90, 1, 101, 1, 111, 1, 118, 2),
                golden("office30", Archetype.OFFICE, 30_000_000, 7, 1, 35, 1, 53, 1, 85, 1, 94, 1, 104, 1),
                golden("prog250", Archetype.DEV, 250_000_000, 13, 1, 40, 1, 60, 1, 76, 1, 90, 1, 89, 1, 97, 1, 109, 1, 116, 1));
    }

    private static Arguments golden(String code, Archetype archetype, long budget, int... variantAndQuantity) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (int index = 0; index < variantAndQuantity.length; index += 2) {
            quantities.put(variantAndQuantity[index], variantAndQuantity[index + 1]);
        }
        return Arguments.of(code, archetype, budget, quantities);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenBuilds")
    @DisplayName("Golden test solver (DD 13.3): tương thích, tổng ≤ B_test, priority 1 giữ, U ≥ U(bộ mẫu) − 0,02")
    void solverMatchesExpertBuilds(String code, Archetype archetype, long budget, Map<Integer, Integer> quantities) {
        CompatibilitySnapshot snapshot = facade.snapshot();
        List<BuildLine> sample = new ArrayList<>();
        quantities.forEach((variantId, quantity) -> {
            ComponentCandidateDTO part = snapshot.candidate(variantId).orElseThrow();
            sample.add(new BuildLine(part.getCategorySlug(), part, quantity));
        });
        BigDecimal sampleTotal = sample.stream().map(line -> line.part().getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // B_test = B nếu bộ mẫu ≤ B, ngược lại = tổng bộ mẫu (dòng ⚠)
        BigDecimal testBudget = sampleTotal.compareTo(BigDecimal.valueOf(budget)) <= 0 ? BigDecimal.valueOf(budget) : sampleTotal;
        ValidatedProfile profile = fixture(snapshot, archetype, testBudget);

        long started = System.nanoTime();
        SolverResult result = solve(snapshot, profile, testBudget);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        ScoreContext ctx = ScoreContext.of(profile, facade.buildPool(snapshot, profile, testBudget), snapshot, testBudget,
                presets::defaultPreferences);
        double sampleUtility = partScorer.utility(sample, ctx);
        System.out.printf("[golden] %-10s B=%,d total=%,d (%.1f%%) U=%.4f mẫu U=%.4f reason=%s relaxed=%s %dms%n  %s%n",
                code, testBudget.longValue(), result.total().longValue(),
                result.total().doubleValue() * 100 / testBudget.doubleValue(), result.utility(), sampleUtility,
                result.reasonCode(), result.relaxedItems(), elapsedMs, describe(result.lines()));

        assertThat(result.compatibility().compatible()).as(code).isTrue();
        assertThat(result.total()).as(code).isLessThanOrEqualTo(testBudget);
        assertPriorityOneKept(result, profile);
        boolean nearBudget = result.total().compareTo(testBudget.multiply(new BigDecimal("0.90"))) >= 0;
        assertThat(nearBudget || List.of("NEEDS_SATURATED", "CATALOG_CEILING").contains(result.reasonCode()))
                .as(code + " phải ≥ 90%% ngân sách hoặc có reasonCode bão hòa/trần").isTrue();
        // So U chỉ có nghĩa khi bộ mẫu thỏa chính hồ sơ đó; bộ mẫu vi phạm thì ghi lại để người xem xét (DD 14.4, K6)
        List<String> violations = sampleViolations(sample, profile, snapshot);
        if (violations.isEmpty()) {
            assertThat(result.utility()).as(code + " U so với bộ mẫu").isGreaterThanOrEqualTo(sampleUtility - 0.02);
        } else {
            System.out.printf("[golden] %s: bộ mẫu vi phạm hồ sơ %s, bỏ qua so sánh U%n", code, violations);
        }
    }

    private static List<String> sampleViolations(List<BuildLine> sample, ValidatedProfile profile, CompatibilitySnapshot snapshot) {
        List<String> violations = new ArrayList<>();
        var metrics = BuildMetrics.of(sample, snapshot);
        for (ProfileMetricDto metric : profile.metrics()) {
            if (!ProfileMatcher.matches(metric, metrics)) violations.add(metric.getId() + " " + metric.getLabel());
        }
        for (ProfileConstraintDto constraint : profile.constraints()) {
            for (BuildLine line : sample) {
                if (constraint.getRole().equals(line.role()) && !ProfileMatcher.matches(constraint, line.part(), snapshot)) {
                    violations.add(constraint.getId() + " " + constraint.getLabel());
                }
            }
        }
        return violations;
    }

    @Test
    @DisplayName("Quét ngân sách (DD 13.3): 6 hồ sơ mẫu × 25 mức, không fail, tổng ≤ B, dưới 90% phải có reasonCode")
    void budgetSweepKeepsInvariants() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        List<String> report = new ArrayList<>();
        int inWindow = 0;
        int points = 0;
        long slowestMs = 0;
        for (Archetype archetype : Archetype.values()) {
            for (long amount : SWEEP_BUDGETS) {
                BigDecimal budget = BigDecimal.valueOf(amount);
                ValidatedProfile profile = fixture(snapshot, archetype, budget);
                long started = System.nanoTime();
                SolverResult result;
                try {
                    result = solve(snapshot, profile, budget);
                } catch (BuildRuleException e) {
                    assertThat(e.getCode()).as("%s B=%,d: chỉ được ném NO_FEASIBLE_BUILD", archetype, amount).isEqualTo("NO_FEASIBLE_BUILD");
                    report.add(String.format("%-8s %,15d NO_FEASIBLE_BUILD %s", archetype, amount, e.getMessage()));
                    continue;
                }
                long elapsedMs = (System.nanoTime() - started) / 1_000_000;
                slowestMs = Math.max(slowestMs, elapsedMs);
                points++;
                double percent = result.total().doubleValue() * 100 / amount;
                if (percent >= 90) inWindow++;
                report.add(String.format("%-8s %,15d total=%,14d %6.1f%% reason=%-20s relaxed=%s %dms", archetype, amount,
                        result.total().longValue(), percent, result.reasonCode(), result.relaxedItems(), elapsedMs));

                assertThat(result.compatibility().compatible()).as("%s B=%,d", archetype, amount).isTrue();
                assertThat(result.total()).as("%s B=%,d", archetype, amount).isLessThanOrEqualTo(budget);
                if (percent < 90) {
                    assertThat(result.reasonCode()).as("%s B=%,d dưới 90%% phải có reasonCode", archetype, amount).isNotNull();
                }
            }
        }
        report.forEach(line -> System.out.println("[sweep] " + line));
        System.out.printf("[sweep] %d/%d điểm trong cửa sổ 90–100%%, chậm nhất %d ms%n", inWindow, points, slowestMs);
    }

    @Test
    @DisplayName("Tất định: cùng hồ sơ, cùng ngân sách, cùng dữ liệu → cùng bộ máy")
    void solverIsDeterministic() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        ValidatedProfile profile = fixture(snapshot, Archetype.GAMING, budget);

        SolverResult first = solve(snapshot, profile, budget);
        SolverResult second = solve(snapshot, profile, budget);

        assertThat(describe(second.lines())).isEqualTo(describe(first.lines()));
        assertThat(second.total()).isEqualByComparingTo(first.total());
    }

    private ValidatedProfile withBrandRule(CompatibilitySnapshot snapshot, BigDecimal budget, String op, String brand) {
        RequirementProfileDto draft = presets.preset(Archetype.GAMING, BuildTier.of(budget));
        draft.getConstraints().add(ProfileConstraintDto.builder().id("brand").role("ram").spec(ProfileMatcher.BRAND_SPEC)
                .op(op).values(new ArrayList<>(List.of(brand))).priority(1).reason("Khách nói rõ").build());
        return validator.validate(draft, snapshot.dictionary(), ProfileValidator.SOURCE_AI, null);
    }

    private static List<String> brandsOf(SolverResult result, String role) {
        return result.lines().stream().filter(line -> role.equals(line.role()))
                .map(line -> line.part().getBrandName()).toList();
    }

    @Test
    @DisplayName("Bắt buộc hãng theo linh kiện: mọi RAM đúng hãng, linh kiện khác không bị ràng buộc theo")
    void requiredBrandFiltersOnlyThatRole() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);

        SolverResult result = solve(snapshot, withBrandRule(snapshot, budget, "IN", "kingston"), budget);

        assertThat(brandsOf(result, "ram")).isNotEmpty().containsOnly("Kingston");
        assertThat(result.lines().stream().filter(line -> !"ram".equals(line.role())).map(line -> line.part().getBrandName()))
                .isNotEmpty().anyMatch(brand -> !"Kingston".equals(brand));
    }

    @Test
    @DisplayName("Loại hãng theo linh kiện: RAM không có hãng đó, các linh kiện khác vẫn được dùng hãng đó")
    void excludedBrandAppliesOnlyToThatRole() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        ValidatedProfile profile = withBrandRule(snapshot, budget, "NOT_IN", "Corsair");

        SolverResult result = solve(snapshot, profile, budget);

        assertThat(brandsOf(result, "ram")).isNotEmpty().doesNotContain("Corsair");
        assertThat(facade.buildPool(snapshot, profile, budget).of("psu"))
                .anyMatch(part -> "Corsair".equals(part.getBrandName()));
    }

    @Test
    @DisplayName("Hãng bắt buộc không đủ đáp ứng yêu cầu priority 1: báo NO_FEASIBLE_BUILD, không âm thầm đổi hãng")
    void requiredBrandThatCannotBeMetIsReported() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        RequirementProfileDto draft = presets.preset(Archetype.GAMING, BuildTier.of(budget));
        draft.getConstraints().add(ProfileConstraintDto.builder().id("brand").role("ram").spec(ProfileMatcher.BRAND_SPEC)
                .op("IN").values(new ArrayList<>(List.of("Kingston"))).priority(1).reason("Chỉ dùng RAM Kingston").build());
        draft.getConstraints().add(ProfileConstraintDto.builder().id("ddr4").role("ram").spec("Capacity_GB")
                .op("GTE").value(96.0).priority(1).reason("Mỗi thanh ít nhất 96 GB").build());
        ValidatedProfile profile = validator.validate(draft, snapshot.dictionary(), ProfileValidator.SOURCE_AI, null);

        assertThatThrownBy(() -> solve(snapshot, profile, budget))
                .isInstanceOfSatisfying(BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("NO_FEASIBLE_BUILD"));
    }

    @Test
    @DisplayName("Validator: hãng lạ bị bỏ và báo lại; preference theo Brand chuyển về brands.prefer")
    void validatorHandlesBrandRules() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);

        ValidatedProfile unknown = withBrandRule(snapshot, budget, "IN", "HangKhongTonTai");
        assertThat(unknown.constraints()).noneMatch(item -> ProfileMatcher.BRAND_SPEC.equals(item.getSpec()));
        assertThat(unknown.profile().getDroppedItems()).anyMatch(item -> "brand".equals(item.getId()));
        assertThat(unknown.profile().getUnmetNeeds()).anyMatch(need -> need.contains("HangKhongTonTai"));

        ValidatedProfile known = withBrandRule(snapshot, budget, "IN", "Kingston");
        assertThat(known.constraints()).filteredOn(item -> ProfileMatcher.BRAND_SPEC.equals(item.getSpec()))
                .singleElement().satisfies(item -> assertThat(item.getLabel()).contains("Hãng: Kingston"));

        RequirementProfileDto both = presets.preset(Archetype.GAMING, BuildTier.of(budget));
        both.getConstraints().add(ProfileConstraintDto.builder().id("brand").role("ram").spec(ProfileMatcher.BRAND_SPEC)
                .op("IN").values(new ArrayList<>(List.of("Kingston"))).priority(1).build());
        both.getBrands().getPrefer().add("Kingston");
        assertThat(validator.validate(both, snapshot.dictionary(), ProfileValidator.SOURCE_AI, null).profile().getBrands().getPrefer()).isEmpty();

        RequirementProfileDto soft = presets.preset(Archetype.GAMING, BuildTier.of(budget));
        soft.getPreferences().add(ProfilePreferenceDto.builder().role("ram").spec(ProfileMatcher.BRAND_SPEC)
                .direction("PREFER").values(new ArrayList<>(List.of("kingston"))).weight(0.5).build());
        ValidatedProfile softened = validator.validate(soft, snapshot.dictionary(), ProfileValidator.SOURCE_AI, null);
        assertThat(softened.profile().getBrands().getPrefer()).containsExactly("Kingston");
        assertThat(softened.profile().getPreferences()).noneMatch(item -> ProfileMatcher.BRAND_SPEC.equals(item.getSpec()));
    }

    @Test
    @DisplayName("Ví dụ 6.10 (B = 30 tr, GAMING): không trả bộ 30.608.000 vượt ngân sách, priority 1 case trắng luôn giữ")
    void gamingThirtyMillionStaysWithinBudget() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        SolverResult result = solve(snapshot, fixture(snapshot, Archetype.GAMING, budget), budget);
        assertThat(result.total()).isLessThanOrEqualTo(budget);
        assertThat(result.total()).isNotEqualByComparingTo("30608000");
        assertThat(result.lines()).noneMatch(line -> "gpu".equals(line.role()) && line.quantity() > 1);
    }

    @Test
    @DisplayName("OFFICE ngân sách 1 tỷ → NEEDS_SATURATED, không GPU rời, mức bão hòa thật được ghi làm ngưỡng")
    void officeSaturates() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(1_000_000_000);
        SolverResult result = solve(snapshot, fixture(snapshot, Archetype.OFFICE, budget), budget);
        System.out.printf("[office 1 tỷ] total=%,d reason=%s %s%n", result.total().longValue(), result.reasonCode(), describe(result.lines()));
        assertThat(result.reasonCode()).isIn("NEEDS_SATURATED", "CATALOG_CEILING");
        assertThat(result.total()).isLessThan(budget.multiply(new BigDecimal("0.90")));
        assertThat(result.compatibility().compatible()).isTrue();
        assertThat(result.lines()).noneMatch(line -> "gpu".equals(line.role()));
        // DD ước "khoảng 29 tr" (mốc gần đúng, 13.3: chạy lại rồi ghi số thật). Số thật cao hơn vì ưu tiên mặc định
        // Bus_Speed, M2_Slots, RAM_Slots, kích thước vỏ, radiator không có cap: mỗi bước vẫn làm U tăng > EPS.
        assertThat(result.total()).isLessThanOrEqualTo(BigDecimal.valueOf(133_000_000));
    }

    @Test
    @DisplayName("GAMING ngân sách 1 tỷ (AC5) → không còn bộ khoảng 30–50 tr, không bao giờ 2 GPU")
    void gamingTopIsNotCheap() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(1_000_000_000);
        SolverResult result = solve(snapshot, fixture(snapshot, Archetype.GAMING, budget), budget);
        System.out.printf("[gaming 1 tỷ] total=%,d reason=%s %s%n", result.total().longValue(), result.reasonCode(), describe(result.lines()));
        assertThat(result.total()).isGreaterThan(BigDecimal.valueOf(150_000_000));
        assertThat(result.reasonCode()).isIn("NEEDS_SATURATED", "CATALOG_CEILING");
        assertThat(result.lines().stream().filter(line -> "gpu".equals(line.role())).mapToInt(BuildLine::quantity).sum()).isEqualTo(1);
        // Cap VRAM 32 GB (DD 5.5): RTX PRO 6000 96 GB (#79) không làm game tốt hơn RTX 5090, không được chọn
        assertThat(result.lines()).noneMatch(line -> line.part().getVariantId() == 79);
    }

    @Test
    @DisplayName("Priority 1 không thỏa được với kho → NO_FEASIBLE_BUILD có blockingItems và minPriority1Price")
    void impossiblePriorityOneThrows() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        var raw = presets.preset(Archetype.GAMING, BuildTier.T3);
        raw.getMetrics().add(ProfileMetricDto.builder().metric("TOTAL_VRAM_GB").op("GTE").value(96.0).priority(1).reason("Cần 96 GB VRAM").build());
        ValidatedProfile profile = validator.validate(raw, snapshot.dictionary(), ProfileValidator.SOURCE_USER_EDITED, null);

        try {
            solve(snapshot, profile, budget);
            fail("phải ném NO_FEASIBLE_BUILD");
        } catch (BuildRuleException e) {
            assertThat(e.getCode()).isEqualTo("NO_FEASIBLE_BUILD");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) e.getData();
            assertThat((List<?>) data.get("blockingItems")).isNotEmpty();
            assertThat(data.get("minPriority1Price")).isNotNull();
            assertThat((BigDecimal) data.get("minPriority1Price")).isGreaterThan(budget);
        }
    }

    @Test
    @DisplayName("Hồ sơ Gemini thật (game 2K, case trắng p1, Perf ≥ 50, không requiredRoles) ở B = 30 tr: vẫn có GPU, hạ Perf thay vì bỏ GPU")
    void realGeminiGamingProfileKeepsGpu() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        var raw = com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto.builder()
                .archetype("GAMING").summary("Máy chơi game 2K, stream và thích case trắng").confidence(0.9)
                .constraints(new ArrayList<>(List.of(
                        ProfileConstraintDto.builder().role("gpu").spec("Perf_Index_1440p").op("GTE").value(50.0).priority(2).reason("Chơi 2K").build(),
                        ProfileConstraintDto.builder().role("case").spec("Color").op("EQ").values(new ArrayList<>(List.of("WHITE"))).priority(1).reason("Case trắng").build())))
                .metrics(new ArrayList<>(List.of(
                        ProfileMetricDto.builder().metric("TOTAL_RAM_GB").op("GTE").value(32.0).priority(2).reason("Stream").build(),
                        ProfileMetricDto.builder().metric("GPU_COUNT").op("EQ").value(1.0).priority(2).reason("Một card").build())))
                .build();
        ValidatedProfile profile = validator.validate(presets.fillMissing(raw, BuildTier.T3), snapshot.dictionary(),
                ProfileValidator.SOURCE_AI, null);

        SolverResult result = solve(snapshot, profile, budget);
        System.out.printf("[gemini 30tr] total=%,d reason=%s relaxed=%s warnings=%s %s%n", result.total().longValue(),
                result.reasonCode(), result.relaxedItems(), result.warnings(), describe(result.lines()));

        assertThat(result.total()).isLessThanOrEqualTo(budget);
        assertThat(result.lines()).anyMatch(line -> "gpu".equals(line.role()));
        assertThat(result.lines()).filteredOn(line -> "case".equals(line.role()))
                .allMatch(line -> "WHITE".equals(snapshot.parsed(line.part()).raw().get(61)));
        assertThat(result.relaxedItems()).doesNotContain("required:gpu", "c2");
    }

    private ValidatedProfile maxPriceFixture(CompatibilitySnapshot snapshot, BigDecimal budget) {
        var raw = presets.preset(Archetype.AI_DATA, BuildTier.of(budget));
        raw.setObjective("MAX_PRICE");
        return validator.validate(raw, snapshot.dictionary(), ProfileValidator.SOURCE_AI, null);
    }

    @Test
    @DisplayName("MAX_PRICE 4 tỷ: tương thích, không thấp hơn bộ đắt ghép tay từ socket khác bộ rẻ nhất, báo MAX_PRICE_REACHED")
    void maxPriceReachesExpensiveBuild() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(4_000_000_000L);
        // Mốc độc lập với solver: Threadripper 9985WX, WRX90, 8 RDIMM, 3× RTX PRO 6000, 4 SSD 8TB, nguồn 2800W
        BigDecimal reference = BigDecimal.ZERO;
        List<BuildLine> sample = new ArrayList<>();
        for (int[] item : new int[][]{{27, 1}, {48, 1}, {62, 8}, {79, 3}, {91, 4}, {101, 1}, {111, 1}, {117, 1}}) {
            ComponentCandidateDTO part = snapshot.candidate(item[0]).orElseThrow();
            sample.add(new BuildLine(part.getCategorySlug(), part, item[1]));
            reference = reference.add(part.getPrice().multiply(BigDecimal.valueOf(item[1])));
        }

        long started = System.nanoTime();
        SolverResult result = solve(snapshot, maxPriceFixture(snapshot, budget), budget);
        System.out.printf("[max-price 4 tỷ] total=%,d mốc=%,d %dms %s%n", result.total().longValue(), reference.longValue(),
                (System.nanoTime() - started) / 1_000_000, describe(result.lines()));

        assertThat(result.compatibility().compatible()).isTrue();
        assertThat(result.total()).isLessThanOrEqualTo(budget);
        assertThat(result.total()).isGreaterThanOrEqualTo(reference);
        assertThat(result.reasonCode()).isEqualTo("MAX_PRICE_REACHED");
        assertWithinStock(result);
    }

    private static void assertWithinStock(SolverResult result) {
        assertThat(result.lines()).allSatisfy(line -> assertThat(line.quantity())
                .as("tồn kho của #%d", line.part().getVariantId()).isLessThanOrEqualTo(line.part().getStockQuantity()));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {30_000_000L, 100_000_000L, 500_000_000L})
    @DisplayName("MAX_PRICE ngân sách vừa/nhỏ: không vượt B, tương thích, không rẻ hơn chế độ thường cùng hồ sơ")
    void maxPriceStaysWithinBudget(long amount) {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(amount);
        SolverResult result = solve(snapshot, maxPriceFixture(snapshot, budget), budget);
        SolverResult normal = solve(snapshot, fixture(snapshot, Archetype.AI_DATA, budget), budget);
        System.out.printf("[max-price %,d] total=%,d thường=%,d reason=%s relaxed=%s %s%n", amount, result.total().longValue(),
                normal.total().longValue(), result.reasonCode(), result.relaxedItems(), describe(result.lines()));

        assertThat(result.compatibility().compatible()).isTrue();
        assertThat(result.total()).isLessThanOrEqualTo(budget);
        assertThat(result.total()).isGreaterThanOrEqualTo(normal.total());
        assertWithinStock(result);
    }

    @Test
    @DisplayName("MAX_PRICE vẫn giữ yêu cầu bắt buộc của khách: chỉ 1 GPU thì không được thêm card để tăng tiền")
    void maxPriceKeepsHardRequirements() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal budget = BigDecimal.valueOf(4_000_000_000L);
        var raw = presets.preset(Archetype.AI_DATA, BuildTier.of(budget));
        raw.setObjective("MAX_PRICE");
        raw.setMetrics(new ArrayList<>(List.of(
                ProfileMetricDto.builder().metric("GPU_COUNT").op("EQ").value(1.0).priority(1).reason("Chỉ một card").build())));
        ValidatedProfile profile = validator.validate(raw, snapshot.dictionary(), ProfileValidator.SOURCE_AI, null);

        SolverResult result = solve(snapshot, profile, budget);

        assertThat(result.lines().stream().filter(line -> "gpu".equals(line.role())).mapToInt(BuildLine::quantity).sum()).isEqualTo(1);
    }

    @Test
    @DisplayName("floorPrice với hồ sơ rỗng = bộ rẻ nhất hợp lệ (DD 6.8), con số thật trên dữ liệu 1–118")
    void floorPriceMatchesCheapestBuild() {
        // DD ghi 6.969.000 (bo A520M-K V2 #30). Dữ liệu hiện tại có bo ASUS PRIME A520M-K #31 đủ thông số và rẻ hơn:
        // Athlon 3000G 1.299.000 + PRIME A520M-K 1.399.000 + 8GB DDR4 1.850.000 + FA100 1.250.000 + PF450D 690.000 + EDRA 290.000
        assertThat(solver.floorPrice(facade.snapshot())).isEqualByComparingTo("6778000");
    }

    private static void assertPriorityOneKept(SolverResult result, ValidatedProfile original) {
        // priority 1 không bao giờ bị hạ (DD 6.7)
        for (ProfileConstraintDto constraint : original.constraints()) {
            if (Objects.equals(constraint.getPriority(), 1)) {
                assertThat(result.relaxedItems()).doesNotContain(constraint.getId());
            }
        }
        for (ProfileMetricDto metric : original.metrics()) {
            if (Objects.equals(metric.getPriority(), 1)) {
                assertThat(result.relaxedItems()).doesNotContain(metric.getId());
            }
        }
    }

    private static String describe(List<BuildLine> lines) {
        return lines.stream().map(line -> line.role() + "=" + line.part().getVariantId() + (line.quantity() > 1 ? "x" + line.quantity() : ""))
                .collect(Collectors.joining(" "));
    }
}
