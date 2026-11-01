package com.project.cybershop.pcbuild;

import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test độ phủ (DD 13.3) trên toàn bộ 139 biến thể của docs/Metadata.sql (pcbuild/coverage-seed.sql), gồm nhóm
 * 119–121 và 18 biến thể GIẢ LẬP 122–139: chỉ các nhóm này có linh kiện trắng, bo Mini-ITX, card blower 24/48 GB,
 * EPYC 9255. DB H2 riêng (pcbuild_coverage) để không lẫn với golden test chạy trên biến thể 1–118.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pcbuild_coverage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:purchase-schema.sql,classpath:pcbuild/pcbuild-schema.sql",
        "spring.sql.init.data-locations=classpath:pcbuild/coverage-seed.sql"
})
@DisplayName("BuildSolver - test độ phủ trên 139 biến thể (DD 13.3, coverage-seed.sql)")
class PcBuildCoverageIntegrationTest {

    // id thông số trong docs/Metadata.sql
    private static final int SOCKET = 1;
    private static final int FORM_FACTOR = 7;
    private static final int VRAM_GB = 12;
    private static final int SUPPORTED_SOCKETS = 21;
    private static final int HAS_IGPU = 26;
    private static final int CPU_SOCKET_COUNT = 31;
    private static final int PCIE_X16_SLOTS = 32;
    private static final int GPU_COOLER_DESIGN = 53;
    private static final int COLOR = 61;
    private static final List<String> WHITE_ROLES = List.of("mainboard", "ram", "case", "psu");

    @Autowired
    private CandidateRepositoryFacade facade;

    @Autowired
    private BuildSolver solver;

    @Autowired
    private ProfilePresets presets;

    @Autowired
    private ProfileValidator validator;

    @Test
    @DisplayName("Dữ liệu: nạp đủ 139 biến thể, có nhóm giả lập (bo Mini-ITX, card blower 48 GB, EPYC 9255)")
    void coverageSeedLoadsAllVariants() {
        CompatibilitySnapshot snapshot = facade.snapshot();
        assertThat(snapshot.candidate(125)).as("bo Mini-ITX AM5 giả lập").isPresent();
        assertThat(snapshot.candidate(134)).as("RTX PRO 5000 48GB blower giả lập").isPresent();
        assertThat(snapshot.candidate(138)).as("EPYC 9255 giả lập").isPresent();
        assertThat(snapshot.candidate(139)).as("biến thể trắng 139 của sản phẩm 98").isPresent();
    }

    @Test
    @DisplayName("Cả bộ trắng không GPU: mainboard, RAM, vỏ, nguồn trắng (priority 1), loại card rời → CPU có iGPU")
    void allWhiteWithoutGpu() {
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        SolverResult result = solveCase("trắng, không GPU", Archetype.OFFICE, budget, profile -> {
            WHITE_ROLES.forEach(role -> profile.getConstraints().add(white(role)));
            profile.getExcludedRoles().add(ProfileRoleRefDto.builder().role("gpu").reason("Không cần card rời").build());
        });

        assertAllWhite(result, WHITE_ROLES);
        assertThat(lines(result, "gpu")).isEmpty();
        assertThat(spec(result, firstLine(result, "cpu"), HAS_IGPU)).as("không card rời thì CPU phải có iGPU").isEqualTo("YES");
    }

    @Test
    @DisplayName("Cả bộ trắng có GPU và tản trắng: thêm gpu, cooler trắng (priority 1)")
    void allWhiteWithGpuAndCooler() {
        BigDecimal budget = BigDecimal.valueOf(60_000_000);
        List<String> roles = List.of("mainboard", "ram", "case", "psu", "gpu", "cooler");
        SolverResult result = solveCase("trắng, có GPU + tản", Archetype.GAMING, budget, profile -> {
            roles.forEach(role -> profile.getConstraints().add(white(role)));
            profile.getRequiredRoles().add(ProfileRoleRefDto.builder().role("gpu").priority(1).reason("Chơi game").build());
            profile.getRequiredRoles().add(ProfileRoleRefDto.builder().role("cooler").priority(1).reason("Tản trắng").build());
        });

        assertThat(lines(result, "gpu")).isNotEmpty();
        assertThat(lines(result, "cooler")).isNotEmpty();
        assertAllWhite(result, roles);
    }

    @Test
    @DisplayName("Vỏ trắng ở ngân sách 8.000.000 ₫ (mức tối thiểu): vẫn có lời giải ≤ 8 tr với vỏ trắng")
    void whiteCaseAtMinimumBudget() {
        BigDecimal budget = BigDecimal.valueOf(8_000_000);
        SolverResult result = solveCase("vỏ trắng 8 tr", Archetype.GENERAL, budget,
                profile -> profile.getConstraints().add(white("case")));

        assertAllWhite(result, List.of("case"));
    }

    @ParameterizedTest(name = "Mini-ITX {0}")
    @CsvSource({"AM5", "LGA1700"})
    @DisplayName("Máy Mini-ITX (AMD và Intel): bo MINI_ITX đúng socket, vỏ hỗ trợ ITX (R6 qua engine)")
    void miniItx(String socket) {
        BigDecimal budget = BigDecimal.valueOf(30_000_000);
        SolverResult result = solveCase("Mini-ITX " + socket, Archetype.GAMING, budget, profile -> {
            profile.getConstraints().add(constraint("mainboard", "Form_Factor", "MINI_ITX"));
            profile.getConstraints().add(constraint("mainboard", "Socket", socket));
        });

        BuildLine board = firstLine(result, "mainboard");
        assertThat(spec(result, board, FORM_FACTOR)).isEqualTo("MINI_ITX");
        assertThat(spec(result, board, SOCKET)).isEqualTo(socket);
    }

    @ParameterizedTest(name = "{0} GPU")
    @CsvSource({"3", "4"})
    @DisplayName("3 và 4 GPU: mọi card kiểu BLOWER (J5c), bo đủ khe PCIe x16 (J5a)")
    void multiGpu(int gpuCount) {
        BigDecimal budget = BigDecimal.valueOf(700_000_000);
        SolverResult result = solveCase(gpuCount + " GPU", Archetype.AI_DATA, budget,
                profile -> profile.getMetrics().add(metric("GPU_COUNT", gpuCount)));

        assertThat(gpuUnits(result)).isGreaterThanOrEqualTo(gpuCount);
        assertThat(lines(result, "gpu")).allSatisfy(line ->
                assertThat(spec(result, line, GPU_COOLER_DESIGN)).as("card %s", line.part().getVariantId()).isEqualTo("BLOWER"));
        assertThat(Integer.parseInt(spec(result, firstLine(result, "mainboard"), PCIE_X16_SLOTS))).isGreaterThanOrEqualTo(gpuUnits(result));
    }

    @ParameterizedTest(name = "VRAM ≥ {0} GB")
    @CsvSource({"24, 150000000", "48, 300000000"})
    @DisplayName("VRAM 24 GB / 48 GB: mọi card đạt VRAM yêu cầu")
    void largeVram(int vramGb, long budgetAmount) {
        BigDecimal budget = BigDecimal.valueOf(budgetAmount);
        SolverResult result = solveCase("VRAM ≥ " + vramGb, Archetype.AI_DATA, budget, profile ->
                profile.getConstraints().add(ProfileConstraintDto.builder().role("gpu").spec("VRAM_GB").op("GTE")
                        .value((double) vramGb).priority(1).reason("Model lớn").build()));

        assertThat(lines(result, "gpu")).isNotEmpty().allSatisfy(line ->
                assertThat(Integer.parseInt(spec(result, line, VRAM_GB))).isGreaterThanOrEqualTo(vramGb));
    }

    @Test
    @DisplayName("2 CPU: bo 2 socket (J6a), đúng 2 CPU và 2 tản (J6b)")
    void dualCpu() {
        BigDecimal budget = BigDecimal.valueOf(600_000_000);
        SolverResult result = solveCase("2 CPU", Archetype.DEV, budget,
                profile -> profile.getMetrics().add(metric("CPU_COUNT", 2)));

        assertThat(units(result, "cpu")).isEqualTo(2);
        // DD 13.3: EPYC 9355 (#29) hoặc 9255 (#138) + MZ73-LM2 (#49) + 2 tản SP5
        assertThat(lines(result, "cpu")).allSatisfy(line -> assertThat(line.part().getVariantId()).isIn(29, 138));
        BuildLine board = firstLine(result, "mainboard");
        assertThat(board.part().getVariantId()).isEqualTo(49);
        assertThat(spec(result, board, CPU_SOCKET_COUNT)).isEqualTo("2");
        assertThat(units(result, "cooler")).isEqualTo(2);
        assertThat(lines(result, "cooler")).allSatisfy(line ->
                assertThat(spec(result, line, SUPPORTED_SOCKETS)).contains("SP5"));
    }

    @Test
    @DisplayName("TOTAL_VRAM_GB ≥ 96 (DD 13.1): 2× RTX PRO 5000 48 GB, không phải 1× RTX PRO 6000 hay 4× RTX PRO 4000")
    void totalVramNinetySix() {
        BigDecimal budget = BigDecimal.valueOf(700_000_000);
        SolverResult result = solveCase("TOTAL_VRAM ≥ 96", Archetype.AI_DATA, budget,
                profile -> profile.getMetrics().add(metric("TOTAL_VRAM_GB", 96)));

        assertThat(lines(result, "gpu")).singleElement().satisfies(line -> {
            assertThat(line.part().getVariantId()).isEqualTo(134);
            assertThat(line.quantity()).isEqualTo(2);
        });
    }

    // ---------- helper ----------

    private CompatibilitySnapshot snapshot;

    /** Hồ sơ mẫu của mức ngân sách + yêu cầu riêng của trường hợp (priority 1, khách sửa), rồi chạy solver. */
    private SolverResult solveCase(String name, Archetype archetype, BigDecimal budget, Consumer<RequirementProfileDto> customize) {
        snapshot = facade.snapshot();
        RequirementProfileDto raw = presets.preset(archetype, BuildTier.of(budget));
        customize.accept(raw);
        ValidatedProfile profile = validator.validate(raw, snapshot.dictionary(), ProfileValidator.SOURCE_USER_EDITED, null);
        assertThat(profile.profile().getDroppedItems()).as("%s: yêu cầu thêm vào phải hợp lệ với từ điển", name).isEmpty();

        long started = System.nanoTime();
        SolverResult result = solver.solve(new SolverInput(budget, profile, facade.buildPool(snapshot, profile, budget), snapshot));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        System.out.printf("[coverage] %-18s B=%,d total=%,d (%.1f%%) reason=%s relaxed=%s %dms%n  %s%n", name,
                budget.longValue(), result.total().longValue(), result.total().doubleValue() * 100 / budget.doubleValue(),
                result.reasonCode(), result.relaxedItems(), elapsedMs, describe(result.lines()));

        // Bất biến chung (DD 13.3): tương thích, không vượt ngân sách, không hạ mục priority 1
        assertThat(result.compatibility().compatible()).as("%s: không có check fail %s", name,
                result.compatibility().checks().stream().filter(check -> "fail".equals(check.getStatus()))
                        .map(check -> check.getKey() + " " + check.getDetail()).toList()).isTrue();
        assertThat(result.total()).as(name).isLessThanOrEqualTo(budget);
        List<String> priorityOne = new ArrayList<>();
        profile.constraints().stream().filter(item -> Objects.equals(item.getPriority(), 1)).forEach(item -> priorityOne.add(item.getId()));
        profile.metrics().stream().filter(item -> Objects.equals(item.getPriority(), 1)).forEach(item -> priorityOne.add(item.getId()));
        assertThat(result.relaxedItems()).as("%s: priority 1 không bao giờ bị hạ", name).doesNotContainAnyElementsOf(priorityOne);
        if (result.total().compareTo(budget.multiply(new BigDecimal("0.90"))) < 0) {
            assertThat(result.reasonCode()).as("%s: dưới 90%% ngân sách phải có reasonCode", name).isNotNull();
        }
        return result;
    }

    private static ProfileConstraintDto white(String role) {
        return constraint(role, "Color", "WHITE");
    }

    private static ProfileConstraintDto constraint(String role, String spec, String code) {
        return ProfileConstraintDto.builder().role(role).spec(spec).op("IN").values(new ArrayList<>(List.of(code)))
                .priority(1).reason("Khách yêu cầu").build();
    }

    private static ProfileMetricDto metric(String metric, double value) {
        return ProfileMetricDto.builder().metric(metric).op("GTE").value(value).priority(1).reason("Khách yêu cầu").build();
    }

    private void assertAllWhite(SolverResult result, List<String> roles) {
        for (String role : roles) {
            assertThat(lines(result, role)).as("phải có %s", role).isNotEmpty().allSatisfy(line ->
                    assertThat(spec(result, line, COLOR)).as("%s #%s", role, line.part().getVariantId()).isEqualTo("WHITE"));
        }
    }

    private String spec(SolverResult result, BuildLine line, int specId) {
        return snapshot.parsed(line.part()).raw().get(specId);
    }

    private static List<BuildLine> lines(SolverResult result, String role) {
        return result.lines().stream().filter(line -> role.equals(line.role())).toList();
    }

    private static BuildLine firstLine(SolverResult result, String role) {
        List<BuildLine> found = lines(result, role);
        assertThat(found).as("phải có %s", role).isNotEmpty();
        return found.get(0);
    }

    private static int units(SolverResult result, String role) {
        return lines(result, role).stream().mapToInt(BuildLine::quantity).sum();
    }

    private static int gpuUnits(SolverResult result) {
        return units(result, "gpu");
    }

    private static String describe(List<BuildLine> lines) {
        return lines.stream().map(line -> line.role() + "=" + line.part().getVariantId() + (line.quantity() > 1 ? "x" + line.quantity() : ""))
                .collect(Collectors.joining(" "));
    }
}
