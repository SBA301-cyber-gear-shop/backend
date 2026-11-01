package com.project.cybershop.pcbuild;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.SolverInput;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chạy đúng phản hồi thô Gemini đã trả (pcbuild/gemini-raw/*.json, thu ngày 30/10/2026, chưa qua xử lý) qua cùng đường
 * với /ai-suggest: fillMissing → ProfileValidator → BuildSolver. Các file này có dạng thật mà hồ sơ mẫu không có:
 * thiếu khóa gpu/psu/case/cooler trong roleWeights, preference có cả spec lẫn metric. Test bằng hồ sơ mẫu không bắt được.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pcbuild;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:purchase-schema.sql,classpath:pcbuild/pcbuild-schema.sql",
        "spring.sql.init.data-locations=classpath:pcbuild-seed.sql"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@DisplayName("Hồ sơ thô của Gemini đi qua fill → validator → solver (dữ liệu biến thể 1–118)")
class PcBuildGeminiRawProfileTest {

    private static final int PERF_TIER = 48;
    private static final int VRAM_GB = 12;
    private static final int RTX_PRO_6000 = 79;

    @Autowired
    private CandidateRepositoryFacade facade;

    @Autowired
    private BuildSolver solver;

    @Autowired
    private ProfilePresets presets;

    @Autowired
    private ProfileValidator validator;

    private final ObjectMapper json = new ObjectMapper();

    private ValidatedProfile prepare(String file, long budget) throws IOException {
        try (InputStream in = new ClassPathResource("pcbuild/gemini-raw/" + file + ".json").getInputStream()) {
            RequirementProfileDto raw = json.readValue(in, RequirementProfileDto.class);
            return validator.validate(presets.fillMissing(raw, BuildTier.of(BigDecimal.valueOf(budget))),
                    facade.snapshot().dictionary(), ProfileValidator.SOURCE_AI, null);
        }
    }

    private SolverResult solve(ValidatedProfile profile, long budget) {
        CompatibilitySnapshot snapshot = facade.snapshot();
        BigDecimal amount = BigDecimal.valueOf(budget);
        SolverResult result = solver.solve(new SolverInput(amount, profile, facade.buildPool(snapshot, profile, amount), snapshot));
        System.out.printf("[gemini-raw] B=%,d total=%,d (%.1f%%) reason=%s relaxed=%s%n  %s%n", budget, result.total().longValue(),
                result.total().doubleValue() * 100 / budget, result.reasonCode(), result.relaxedItems(),
                result.lines().stream().map(line -> line.role() + "=" + line.part().getVariantId()
                        + (line.quantity() > 1 ? "x" + line.quantity() : "")).collect(Collectors.joining(" ")));
        assertThat(result.compatibility().compatible()).isTrue();
        assertThat(result.total()).isLessThanOrEqualTo(amount);
        return result;
    }

    private static List<BuildLine> gpus(SolverResult result) {
        return result.lines().stream().filter(line -> "gpu".equals(line.role())).toList();
    }

    private int spec(BuildLine line, int specId) {
        return Integer.parseInt(facade.snapshot().parsed(line.part()).raw().get(specId));
    }

    @Test
    @DisplayName("Game 4K, 1 tỷ: bù trọng số GPU, giữ preference hiệu năng; ra RTX 5090, không GTX 1650, không RTX PRO 6000")
    void gaming4kGetsTopGamingGpu() throws IOException {
        ValidatedProfile profile = prepare("gaming-4k", 1_000_000_000L);
        assertThat(profile.weight("gpu")).isEqualTo(0.45, org.assertj.core.data.Offset.offset(1e-9));
        // File thô có {"role":"gpu","spec":"Perf_Index_1440p","metric":"GPU_COUNT"}: phải giữ nghĩa spec
        assertThat(profile.profile().getPreferences()).anyMatch(p -> "gpu".equals(p.getRole()) && "Perf_Index_1440p".equals(p.getSpec()));

        SolverResult result = solve(profile, 1_000_000_000L);

        assertThat(gpus(result)).singleElement().satisfies(line -> {
            assertThat(line.part().getVariantId()).isNotEqualTo(RTX_PRO_6000);
            assertThat(spec(line, PERF_TIER)).isEqualTo(10);
            assertThat(line.quantity()).isEqualTo(1);
        });
        assertThat(result.reasonCode()).isIn("NEEDS_SATURATED", "CATALOG_CEILING");
    }

    @Test
    @DisplayName("Game 2K, 30 tr: GPU nhận trọng số 0,45 và là khoản chi lớn nhất, đạt Perf_Tier của hồ sơ")
    void gaming2kSpendsMostOnGpu() throws IOException {
        ValidatedProfile profile = prepare("gaming-2k", 30_000_000L);
        assertThat(profile.weight("gpu")).isEqualTo(0.45, org.assertj.core.data.Offset.offset(1e-9));

        SolverResult result = solve(profile, 30_000_000L);

        BuildLine gpu = gpus(result).get(0);
        assertThat(spec(gpu, PERF_TIER)).isGreaterThanOrEqualTo(5);
        assertThat(result.lines()).allSatisfy(line -> assertThat(line.part().getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .isLessThanOrEqualTo(gpu.part().getPrice().multiply(BigDecimal.valueOf(gpu.quantity()))));
    }

    @Test
    @DisplayName("Dựng phim 4K, 60 tr: GPU có trọng số, đạt VRAM ≥ 12 GB; hồ sơ CREATIVE không bị cap VRAM của GAMING")
    void creativeKeepsVramPriority() throws IOException {
        ValidatedProfile profile = prepare("creative-4k", 60_000_000L);
        assertThat(profile.weight("gpu")).isGreaterThan(0);

        SolverResult result = solve(profile, 60_000_000L);

        assertThat(spec(gpus(result).get(0), VRAM_GB)).isGreaterThanOrEqualTo(12);
        assertThat(profile.profile().getPreferences()).filteredOn(p -> "VRAM_GB".equals(p.getSpec()))
                .allSatisfy(p -> assertThat(p.getCap()).isNull());
    }

    @Test
    @DisplayName("Văn phòng, 15 tr (Gemini ghi gpu = 0 và excludedRoles): vẫn không có GPU, không bị bù trọng số")
    void officeStaysWithoutGpu() throws IOException {
        ValidatedProfile profile = prepare("office", 15_000_000L);
        assertThat(profile.weight("gpu")).isZero();

        assertThat(gpus(solve(profile, 15_000_000L))).isEmpty();
    }

    @Test
    @DisplayName("Lập trình, 30 tr: tương thích, tổng ≤ B, RAM đạt yêu cầu priority 1")
    void devBuildStaysValid() throws IOException {
        SolverResult result = solve(prepare("dev-web", 30_000_000L), 30_000_000L);

        assertThat(result.relaxedItems()).doesNotContain("m1");
    }

    @Test
    @DisplayName("AI, 150 tr: yêu cầu priority 1 cần tối thiểu trên 150 tr → NO_FEASIBLE_BUILD có minPriority1Price, không ra bộ thiếu GPU")
    void aiTrainingBelowFloorIsRejected() throws IOException {
        ValidatedProfile profile = prepare("ai-train", 150_000_000L);

        assertThatThrownBy(() -> solve(profile, 150_000_000L)).isInstanceOfSatisfying(BuildRuleException.class, error -> {
            assertThat(error.getCode()).isEqualTo("NO_FEASIBLE_BUILD");
            Map<?, ?> data = (Map<?, ?>) error.getData();
            assertThat((BigDecimal) data.get("minPriority1Price")).isGreaterThan(BigDecimal.valueOf(150_000_000L));
        });
    }
}
