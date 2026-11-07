package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.BuildCheck;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.RuleViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Bước 6: từng luật DB, luật nhân số lượng, tản khí/nước, R7, luật khuôn và thứ tự trạng thái. */
class CompatibilityEngineImplTest {

    private final CompatibilityEngineImpl engine = new CompatibilityEngineImpl();

    @Test
    @DisplayName("Bộ PC của ví dụ docx: VALID, tổng 19.900.000đ, còn dư 100.000đ")
    void docxBuild_isValid() {
        BuildCheck check = engine.checkBuild(20_000_000L, docxBuild(), context(catalog()));

        assertThat(check.violations()).isEmpty();
        assertThat(check.status()).isEqualTo(BuildStatus.VALID);
        assertThat(check.total()).isEqualTo(19_900_000L);
        assertThat(check.difference()).isEqualTo(-100_000L);
    }

    @Test
    @DisplayName("R1: CPU AM5 với main LGA1700 → RULE_1")
    void socketMismatch_rule1() {
        List<BuildLine> lines = replace(docxBuild(), byId(catalog(), MAIN_INTEL), 1);

        assertThat(rulesOf(engine.checkRules(lines, rules()))).contains("RULE_1");
    }

    @Test
    @DisplayName("INCLUDES không phân biệt hoa thường và khoảng trắng: CPU 'am5' nằm trong 'AM4, AM5'")
    void includes_ignoresCaseAndSpaces() {
        ComponentCandidateDTO cpu = withSpec(byId(catalog(), CPU_A), 1, "am5");
        ComponentCandidateDTO main = withSpec(byId(catalog(), MAIN_A), 1, "am5");
        ComponentCandidateDTO cooler = withSpec(byId(catalog(), COOLER_A), 21, "AM4, AM5");

        List<RuleViolation> violations = engine.checkRules(List.of(line(cpu, 1), line(main, 1), line(cooler, 1)), rules());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("R5 (luật 6): main MINI_ITX trong case chỉ hỗ trợ STD_ATX, MICRO_ATX → RULE_6")
    void formFactorNotSupported_rule6() {
        ComponentCandidateDTO main = withSpec(byId(catalog(), MAIN_A), 7, "MINI_ITX");
        List<BuildLine> lines = replace(docxBuild(), main, 1);

        assertThat(rulesOf(engine.checkRules(lines, rules()))).containsExactly("RULE_6");
    }

    @Test
    @DisplayName("R3 (luật 7): 3 thanh RAM trên main 2 khe → RULE_7, maxQty = 2")
    void ramSticksExceedSlots_rule7() {
        List<BuildLine> lines = replace(docxBuild(), byId(catalog(), RAM_A), 3);

        BuildCheck check = engine.checkBuild(20_000_000L, lines, context(catalog()));

        assertThat(check.status()).isEqualTo(BuildStatus.INCOMPATIBLE);
        assertThat(rulesOf(check.violations())).containsExactly("RULE_7");
        assertThat(check.violations().get(0).message()).contains("1 × 3").contains("(2)");
        assertThat(maxQtyOf(lines, RAM_A)).isEqualTo(2);
    }

    @Test
    @DisplayName("Luật 8: 2 × 32GB trên main tối đa 32GB → RULE_8, maxQty = 1")
    void ramCapacityExceedsMax_rule8() {
        ComponentCandidateDTO ram = withSpec(byId(catalog(), RAM_A), 9, "32");
        ComponentCandidateDTO main = withSpec(byId(catalog(), MAIN_A), 6, "32");
        List<BuildLine> lines = replace(replace(docxBuild(), main, 1), ram, 2);

        assertThat(rulesOf(engine.checkRules(lines, rules()))).containsExactly("RULE_8");
        assertThat(maxQtyOf(lines, RAM_A)).isEqualTo(1);
    }

    @Test
    @DisplayName("Tản nước cao hơn giới hạn case không bị luật chiều cao; radiator 360 > 240 → RULE_10")
    void liquidCooler_skipsHeightButChecksRadiator() {
        ComponentCandidateDTO liquid = part(COOLER_A, 8, "AIO 360", 650_000, 10,
                Map.of(20, "Liquid", 21, "AM5", 19, "400", 60, "360"));
        List<BuildLine> lines = replace(docxBuild(), liquid, 1);

        assertThat(rulesOf(engine.checkRules(lines, rules()))).containsExactly("RULE_10");
    }

    @Test
    @DisplayName("Tản khí cao 170mm trong case tối đa 160mm → RULE_4")
    void airCoolerTooTall_rule4() {
        ComponentCandidateDTO cooler = withSpec(byId(catalog(), COOLER_A), 19, "170");
        List<BuildLine> lines = replace(docxBuild(), cooler, 1);

        assertThat(rulesOf(engine.checkRules(lines, rules()))).containsExactly("RULE_4");
    }

    @Test
    @DisplayName("GPU thiếu Length_MM → RULE_3 báo thiếu thông số")
    void missingSpec_isViolation() {
        ComponentCandidateDTO gpu = withSpec(byId(catalog(), GPU_A), 11, null);
        List<BuildLine> lines = replace(docxBuild(), gpu, 1);

        List<RuleViolation> violations = engine.checkRules(lines, rules());

        assertThat(rulesOf(violations)).containsExactly("RULE_3");
        assertThat(violations.get(0).message()).contains("thiếu thông số");
    }

    @Test
    @DisplayName("R7: CPU 65W + GPU 160W cần ⌈225 × 1,3⌉ = 293W; nguồn 292W lỗi, 293W qua")
    void psuPower_r7() {
        ComponentCandidateDTO psu292 = withSpec(byId(catalog(), PSU_650), 14, "292");
        ComponentCandidateDTO psu293 = withSpec(byId(catalog(), PSU_650), 14, "293");
        // Bỏ luật 9 (nguồn ≥ Rec_PSU_W 550) để chỉ đo R7
        List<CompatibilityRule> rulesWithoutRecPsu = rules().stream()
                .filter(rule -> rule.getId() != 9).toList();

        assertThat(rulesOf(engine.checkRules(replace(docxBuild(), psu292, 1), rulesWithoutRecPsu)))
                .containsExactly("PSU_POWER");
        assertThat(engine.checkRules(replace(docxBuild(), psu293, 1), rulesWithoutRecPsu)).isEmpty();
    }

    @Test
    @DisplayName("Luật 9: nguồn 500W dưới công suất khuyến nghị 550W của GPU → RULE_9")
    void psuBelowGpuRecommendation_rule9() {
        ComponentCandidateDTO psu = withSpec(byId(catalog(), PSU_650), 14, "500");

        assertThat(rulesOf(engine.checkRules(replace(docxBuild(), psu, 1), rules()))).containsExactly("RULE_9");
    }

    @Test
    @DisplayName("CPU Has_iGPU = NO hoặc UNKNOWN mà không có GPU → MISSING_SLOT; YES thì không lỗi khuôn")
    void requiredIf_gpu() {
        for (String hasIgpu : List.of("NO", "UNKNOWN")) {
            ComponentCandidateDTO cpu = withSpec(byId(catalog(), CPU_A), 26, hasIgpu);
            List<BuildLine> lines = withoutCategory(replace(docxBuild(), cpu, 1), 4);

            BuildCheck check = engine.checkBuild(20_000_000L, lines, context(catalog()));

            assertThat(rulesOf(check.violations())).as("Has_iGPU = " + hasIgpu).containsExactly("MISSING_SLOT");
        }
        ComponentCandidateDTO cpuWithIgpu = withSpec(byId(catalog(), CPU_A), 26, "YES");
        List<BuildLine> lines = withoutCategory(replace(docxBuild(), cpuWithIgpu, 1), 4);

        assertThat(engine.checkBuild(20_000_000L, lines, context(catalog())).status()).isEqualTo(BuildStatus.VALID);
    }

    @Test
    @DisplayName("Luật khuôn: thiếu slot bắt buộc, hai mẫu cùng loại, món ngoài khuôn")
    void templateViolations() {
        List<BuildLine> missingCase = withoutCategory(docxBuild(), 7);
        assertThat(rulesOf(engine.checkBuild(20_000_000L, missingCase, context(catalog())).violations()))
                .contains("MISSING_SLOT");

        List<BuildLine> twoSsd = new ArrayList<>(docxBuild());
        twoSsd.add(line(byId(catalog(), SSD_CHEAP), 1));
        assertThat(rulesOf(engine.checkBuild(20_000_000L, twoSsd, context(catalog())).violations()))
                .contains("DUPLICATE_SLOT");

        List<BuildLine> outside = new ArrayList<>(docxBuild());
        outside.add(new BuildLine(null, part(999, 20, "Loa", 500_000, 5, Map.of()), 1));
        assertThat(rulesOf(engine.checkBuild(20_000_000L, outside, context(catalog())).violations()))
                .contains("NOT_IN_TEMPLATE");
    }

    @Test
    @DisplayName("QTY_RANGE: 2 GPU trên main 1 khe PCIe x16")
    void gpuQuantityExceedsPcieSlots() {
        List<BuildLine> lines = replace(docxBuild(), byId(catalog(), GPU_A), 2);

        BuildCheck check = engine.checkBuild(30_000_000L, lines, context(catalog()));

        assertThat(rulesOf(check.violations())).contains("QTY_RANGE");
        assertThat(maxQtyOf(lines, GPU_A)).isEqualTo(1);
    }

    @Test
    @DisplayName("Không lỗi mà tổng > ngân sách → OVER_BUDGET, difference dương")
    void overBudget() {
        BuildCheck check = engine.checkBuild(19_600_000L, docxBuild(), context(catalog()));

        assertThat(check.status()).isEqualTo(BuildStatus.OVER_BUDGET);
        assertThat(check.difference()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("Vừa xung đột vừa vượt tiền → INCOMPATIBLE (ưu tiên)")
    void incompatibleWinsOverBudget() {
        List<BuildLine> lines = replace(docxBuild(), byId(catalog(), RAM_A), 3);

        BuildCheck check = engine.checkBuild(10_000_000L, lines, context(catalog()));

        assertThat(check.status()).isEqualTo(BuildStatus.INCOMPATIBLE);
        assertThat(check.difference()).isPositive();
    }

    private int maxQtyOf(List<BuildLine> lines, int variantId) {
        BuildLine target = lines.stream().filter(line -> line.part().getVariantId() == variantId).findFirst().orElseThrow();
        List<BuildLine> others = lines.stream().filter(line -> line != target).toList();
        return engine.maxQuantity(target.slot(), target.part(), others, rules());
    }

    /** Đổi món cùng loại với part (hoặc chỉnh số lượng) trong bộ. */
    private static List<BuildLine> replace(List<BuildLine> lines, ComponentCandidateDTO part, int quantity) {
        List<BuildLine> result = new ArrayList<>();
        for (BuildLine line : lines) {
            result.add(line.categoryId().equals(part.getCategoryId()) ? line(part, quantity) : line);
        }
        return result;
    }

    private static List<BuildLine> withoutCategory(List<BuildLine> lines, int categoryId) {
        return lines.stream().filter(line -> line.categoryId() != categoryId).toList();
    }

    private static List<String> rulesOf(List<RuleViolation> violations) {
        return violations.stream().map(RuleViolation::rule).toList();
    }
}
