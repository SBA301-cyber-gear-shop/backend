package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Bước 5: chọn món sát trần, chặn dưới, chừa tiền, quay lui; mỗi ca đều kiểm lại bằng bước 6. */
class BuildSolverImplTest {

    private final CompatibilityEngineImpl engine = new CompatibilityEngineImpl();
    private final BuildSolverImpl solver = new BuildSolverImpl(engine, 200);
    private final BudgetAllocationServiceImpl allocation = new BudgetAllocationServiceImpl();

    @Test
    @DisplayName("Ví dụ docx 20.000.000đ GAMING: đúng 11 món như bảng bước 5, tổng 19.900.000đ")
    void docxExample() {
        List<BuildLine> lines = solve("GAMING", 20_000_000L, catalog());

        assertThat(ids(lines)).containsExactly(MONITOR_A, KEYBOARD_A, MOUSE_A, CPU_A, MAIN_A, RAM_A, SSD_A, GPU_A,
                COOLER_A, CASE_A, PSU_650);
        assertThat(quantityOf(lines, RAM_A)).isEqualTo(2);
        assertThat(total(lines)).isEqualTo(19_900_000L);
        assertValid(20_000_000L, lines, catalog());
    }

    @Test
    @DisplayName("Chặn dưới: main rẻ nhất hợp lệ 2.100.000đ > phần 1.850.000đ → carry −250.000đ, lượt RAM chỉ còn 1.350.000đ")
    void lowerBound_negativeCarryShrinksNextTurn() {
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.replaceAll(part -> part.getVariantId() == MAIN_A
                ? mainboard(MAIN_A, "Main-A", 2_100_000, "AM5", "DDR5", "2", "MICRO_ATX")
                : part);
        int ramCheapDdr5 = 47;
        parts.add(ram(ramCheapDdr5, "RAM DDR5 8GB rẻ", 600_000, "DDR5", "8"));

        List<BuildLine> lines = solve("GAMING", 20_000_000L, parts);

        assertThat(ids(lines)).contains(MAIN_A);
        // RAM-A × 2 = 1.600.000 > 1.350.000 nên lấy mẫu rẻ hơn × 2 = 1.200.000
        assertThat(ids(lines)).contains(ramCheapDdr5).doesNotContain(RAM_A);
        assertThat(quantityOf(lines, ramCheapDdr5)).isEqualTo(2);
        assertValid(20_000_000L, lines, parts);
    }

    @Test
    @DisplayName("Chừa tiền: món vừa trần mềm nhưng làm lượt sau hết tiền thì không được chọn")
    void reserve_keepsMoneyForLaterTurns() {
        // Ngân sách 16tr: phần màn 1.920.000, có màn 1.900.000 vừa trần mềm. Chỉ còn một GPU giá 6.890.000 nên giá rẻ
        // nhất của 10 lượt sau = 7.310.000 + 6.890.000 = 14.200.000 → màn chỉ được tiêu tối đa 1.800.000 → màn rẻ 1.490.000
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.add(part(204, 9, "Màn 1.9tr", 1_900_000, 10, Map.of()));
        parts.replaceAll(part -> part.getVariantId() == GPU_CHEAP ? gpu(GPU_CHEAP, "GPU rẻ", 6_890_000, "240", "120") : part);
        parts.removeIf(part -> part.getVariantId() == GPU_A);

        List<BuildLine> lines = solve("GAMING", 16_000_000L, parts);

        assertThat(ids(lines)).contains(MONITOR_CHEAP).doesNotContain(204);
        assertValid(16_000_000L, lines, parts);
    }

    @Test
    @DisplayName("Quay lui: GPU-A dài 340mm không vừa case nào → lùi về GPU, lấy GPU rẻ hơn một bậc")
    void backtrack_whenNoCaseFitsGpu() {
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.replaceAll(part -> part.getVariantId() == GPU_A ? gpu(GPU_A, "GPU-A dài", 5_600_000, "340", "160") : part);

        List<BuildLine> lines = solve("GAMING", 20_000_000L, parts);

        assertThat(ids(lines)).contains(GPU_CHEAP).doesNotContain(GPU_A);
        assertValid(20_000_000L, lines, parts);
    }

    @Test
    @DisplayName("OFFICE (GPU 0%): không có GPU, chỉ chọn CPU có Has_iGPU = YES")
    void office_skipsGpuAndRequiresIgpu() {
        List<BuildLine> lines = solve("OFFICE", 12_500_000L, catalog());

        assertThat(lines).noneMatch(line -> line.categoryId() == 4);
        assertThat(ids(lines)).contains(CPU_INTEL_IGPU, MAIN_INTEL, RAM_DDR4);
        // Lượt cuối: phần PSU 875.000 < nguồn rẻ nhất 900.000 → chặn dưới nâng lên 900.000
        assertThat(ids(lines)).contains(PSU_550);
        assertValid(12_500_000L, lines, catalog());
    }

    @Test
    @DisplayName("CPU Has_iGPU = UNKNOWN không được chọn khi GPU bị bỏ qua (D3)")
    void office_unknownIgpuIsExcluded() {
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.replaceAll(part -> part.getVariantId() == CPU_INTEL_IGPU
                ? cpu(CPU_INTEL_IGPU, "CPU Intel chưa rõ iGPU", 2_000_000, "LGA1700", "65", "UNKNOWN")
                : part);
        int cpuWithIgpu = 15;
        parts.add(cpu(cpuWithIgpu, "CPU Intel có iGPU", 1_800_000, "LGA1700", "65", "YES"));

        List<BuildLine> lines = solve("OFFICE", 12_500_000L, parts);

        assertThat(ids(lines)).contains(cpuWithIgpu).doesNotContain(CPU_INTEL_IGPU);
    }

    @Test
    @DisplayName("Main chỉ 1 khe RAM → RAM lấy 1 thanh")
    void singleRamSlot_takesOneStick() {
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.replaceAll(part -> part.getVariantId() == MAIN_A
                ? mainboard(MAIN_A, "Main-A 1 khe", 1_800_000, "AM5", "DDR5", "1", "MICRO_ATX")
                : part);

        List<BuildLine> lines = solve("GAMING", 20_000_000L, parts);

        assertThat(quantityOf(lines, RAM_A)).isEqualTo(1);
        assertValid(20_000_000L, lines, parts);
    }

    @Test
    @DisplayName("Hai món bằng giá → lấy món còn nhiều hàng hơn")
    void tie_prefersHigherStock() {
        int monitorMoreStock = 205;
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.add(part(monitorMoreStock, 9, "Màn-A bản nhiều hàng", 2_350_000, 50, Map.of()));

        List<BuildLine> lines = solve("GAMING", 20_000_000L, parts);

        assertThat(ids(lines)).contains(monitorMoreStock).doesNotContain(MONITOR_A);
    }

    @Test
    @DisplayName("Kho không có nguồn → BUILD_NOT_FOUND, data.slot = PSU")
    void noPsu_buildNotFound() {
        List<ComponentCandidateDTO> parts = new ArrayList<>(catalog());
        parts.removeIf(part -> part.getCategoryId() == 6);

        assertThatThrownBy(() -> solve("GAMING", 20_000_000L, parts))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("BUILD_NOT_FOUND");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(422);
                    assertThat(((Map<?, ?>) exception.getData()).get("slot")).isEqualTo("PSU");
                });
    }

    @Test
    @DisplayName("Tính chất: nhiều mức ngân sách đều ra bộ VALID, tổng ≤ ngân sách")
    void manyBudgets_alwaysValidAndWithinBudget() {
        for (long budget : new long[]{16_000_000L, 18_500_000L, 25_000_000L, 40_000_000L, 60_000_000L}) {
            List<BuildLine> lines = solve("GAMING", budget, catalog());

            assertThat(total(lines)).as("budget " + budget).isLessThanOrEqualTo(budget);
            assertValid(budget, lines, catalog());
        }
    }

    private List<BuildLine> solve(String purpose, long budget, List<ComponentCandidateDTO> parts) {
        BuildContext context = context(parts);
        BuildPlan plan = allocation.plan(purpose, budget, context.metadata());
        return solver.solve(budget, plan, context);
    }

    private void assertValid(long budget, List<BuildLine> lines, List<ComponentCandidateDTO> parts) {
        assertThat(engine.checkBuild(budget, lines, context(parts)).status()).isEqualTo(BuildStatus.VALID);
    }

    private static List<Integer> ids(List<BuildLine> lines) {
        return lines.stream().map(line -> line.part().getVariantId()).toList();
    }

    private static int quantityOf(List<BuildLine> lines, int variantId) {
        return lines.stream().filter(line -> line.part().getVariantId() == variantId).findFirst().orElseThrow().quantity();
    }

    private static long total(List<BuildLine> lines) {
        return lines.stream().mapToLong(BuildLine::lineTotal).sum();
    }
}
