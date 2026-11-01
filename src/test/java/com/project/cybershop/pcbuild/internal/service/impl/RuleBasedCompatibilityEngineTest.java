package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;
import com.project.cybershop.pcbuild.internal.model.RuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.RULES;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.lines;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.snapshot;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.validDesktop;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.withSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("RuleBasedCompatibilityEngine - luật R1–R12, J1–J11 trên snapshot dựng tay, không DB")
class RuleBasedCompatibilityEngineTest {

    // Constructor không nhận dependency truy cập dữ liệu nào (DD 7.1, nguyên tắc 7)
    private final RuleBasedCompatibilityEngine engine = new RuleBasedCompatibilityEngine();

    private EngineReport evaluate(List<BuildLine> lines, EvaluationContext.Mode mode) {
        return engine.evaluate(lines, new EvaluationContext(null, mode, snapshot()));
    }

    private EngineReport evaluate(Map<String, ComponentCandidateDTO> parts, EvaluationContext.Mode mode) {
        return evaluate(lines(parts), mode);
    }

    private static List<String> statusesOf(EngineReport report, String key) {
        return report.checks().stream().filter(check -> key.equals(check.getKey()))
                .map(CompatibilityCheckResponse::getStatus).toList();
    }

    @Test
    @DisplayName("Bộ desktop chuẩn: tương thích, mọi luật R1–R12 pass, không unknown")
    void validDesktopPassesEveryRule() {
        EngineReport report = evaluate(validDesktop(), EvaluationContext.Mode.SOLVER);

        assertThat(report.compatible()).isTrue();
        assertThat(report.hasUnknown()).isFalse();
        for (int ruleId = 1; ruleId <= 12; ruleId++) {
            assertThat(statusesOf(report, "R" + ruleId)).as("R" + ruleId).containsOnly("pass");
        }
        assertThat(report.requiredPsuW()).isEqualTo(650); // max(ceil(400 × 1,3) = 520, Rec_PSU_W 650)
        assertThat(report.total()).isEqualByComparingTo("27000000");
        assertThat(report.capacity().getRamSlotsUsed()).isEqualTo(2);
        assertThat(report.capacity().getRamGb()).isEqualTo(32);
        assertThat(report.capacity().getGpuMax()).isEqualTo(1);
    }

    static Stream<Arguments> ruleViolations() {
        return Stream.of(
                Arguments.of(1, "mainboard", 1, "LGA1700"),
                Arguments.of(2, "ram", 4, "DDR4"),
                Arguments.of(3, "gpu", 11, "400"),
                Arguments.of(4, "cooler", 19, "180"),
                Arguments.of(5, "cooler", 21, "LGA1700,LGA1851"),
                Arguments.of(6, "case", 18, "STD_ATX,E_ATX"),
                Arguments.of(7, "ram", 34, "6"),
                Arguments.of(8, "ram", 9, "256"),
                Arguments.of(9, "psu", 14, "600"),
                Arguments.of(10, "cooler", 60, "420"),
                Arguments.of(11, "ssd", 40, "3"),
                Arguments.of(12, "mainboard", 31, "2"));
    }

    @ParameterizedTest(name = "R{0}: {1}.spec{2} = {3} → fail")
    @MethodSource("ruleViolations")
    @DisplayName("Mỗi luật R1–R12 có ca fail")
    void eachRuleFailsWhenViolated(int ruleId, String role, int specId, String badValue) {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put(role, withSpec(parts.get(role), specId, badValue));

        EngineReport report = evaluate(parts, EvaluationContext.Mode.USER);

        assertThat(statusesOf(report, "R" + ruleId)).containsExactly("fail");
        assertThat(report.compatible()).isFalse();
    }

    @Test
    @DisplayName("Thiếu giá trị một vế: SOLVER → fail, USER → unknown (vẫn compatible)")
    void missingValueDependsOnMode() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("mainboard", withSpec(parts.get("mainboard"), 1, null));

        EngineReport solver = evaluate(parts, EvaluationContext.Mode.SOLVER);
        EngineReport user = evaluate(parts, EvaluationContext.Mode.USER);

        assertThat(statusesOf(solver, "R1")).containsExactly("fail");
        assertThat(statusesOf(user, "R1")).containsExactly("unknown");
        assertThat(statusesOf(user, "J10")).containsExactly("unknown");
        assertThat(solver.compatible()).isFalse();
        assertThat(user.compatible()).isTrue();
        assertThat(user.hasUnknown()).isTrue();
    }

    @Test
    @DisplayName("Giá trị số bẩn \"65/88\": không ném lỗi, luật và nguồn ra unknown ở USER")
    void dirtyNumberIsUnknownNotException() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("cpu", withSpec(parts.get("cpu"), 2, "65/88"));
        parts.put("ram", withSpec(parts.get("ram"), 34, "2/2"));

        assertThatCode(() -> evaluate(parts, EvaluationContext.Mode.USER)).doesNotThrowAnyException();
        EngineReport report = evaluate(parts, EvaluationContext.Mode.USER);

        assertThat(statusesOf(report, "R7")).containsExactly("unknown");
        assertThat(statusesOf(report, "J4")).containsExactly("unknown");
        assertThat(report.compatible()).isTrue();
    }

    @Test
    @DisplayName("J11: tản AIO bỏ qua R4 (chiều cao) nhưng vẫn kiểm R5 (socket) và R10 (radiator)")
    void liquidCoolerSkipsHeightRuleOnly() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("cooler", part(8, "cooler", "AIO 360", 3_000_000,
                Map.of(20, "Liquid", 21, "AM5", 60, "360")));

        EngineReport report = evaluate(parts, EvaluationContext.Mode.SOLVER);

        assertThat(statusesOf(report, "R4")).isEmpty();
        assertThat(statusesOf(report, "R5")).containsExactly("pass");
        assertThat(statusesOf(report, "R10")).containsExactly("pass");
        assertThat(statusesOf(report, "J10")).as("thiếu Height_MM được chấp nhận với tản nước").isEmpty();
        assertThat(report.compatible()).isTrue();
    }

    @Test
    @DisplayName("J1: CPU không iGPU và không có GPU rời → fail")
    void cpuWithoutIntegratedGraphicsNeedsGpu() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.remove("gpu");
        parts.put("cpu", withSpec(parts.get("cpu"), 26, "NO"));

        assertThat(statusesOf(evaluate(parts, EvaluationContext.Mode.USER), "J1")).containsExactly("fail");
    }

    @Test
    @DisplayName("J2: CPU tray không kèm tản và không có tản rời → fail")
    void trayCpuNeedsCooler() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.remove("cooler");

        assertThat(statusesOf(evaluate(parts, EvaluationContext.Mode.USER), "J2")).containsExactly("fail");
    }

    @Test
    @DisplayName("J3a: kit 2 thanh × 2 trên bo 2 khe → fail (R7 vẫn pass vì chỉ so 1 kit)")
    void ramModulesCountedByQuantity() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("mainboard", withSpec(parts.get("mainboard"), 5, "2"));
        List<BuildLine> lines = replaceQuantity(lines(parts), "ram", 2);

        EngineReport report = evaluate(lines, EvaluationContext.Mode.USER);

        assertThat(statusesOf(report, "R7")).containsExactly("pass");
        assertThat(statusesOf(report, "J3a")).containsExactly("fail");
    }

    @Test
    @DisplayName("J3c: 2 SSD trên bo 1 khe M.2 → fail")
    void twoSsdsOnSingleM2Slot() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("mainboard", withSpec(parts.get("mainboard"), 8, "1"));
        List<BuildLine> lines = new ArrayList<>(lines(parts));
        lines.add(new BuildLine("ssd", part(15, "ssd", "NVMe 2TB", 3_000_000, Map.of(40, "1")), 1));

        assertThat(statusesOf(evaluate(lines, EvaluationContext.Mode.USER), "J3c")).containsExactly("fail");
    }

    @Test
    @DisplayName("J5a: 2 GPU trên bo 1 khe PCIe x16 → fail")
    void twoGpusOnSingleSlot() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("gpu", withSpec(parts.get("gpu"), 54, "2"));

        EngineReport report = evaluate(replaceQuantity(lines(parts), "gpu", 2), EvaluationContext.Mode.USER);

        assertThat(statusesOf(report, "J5a")).containsExactly("fail");
        assertThat(statusesOf(report, "J5b")).isEmpty();
    }

    @Test
    @DisplayName("J5c: 3 GPU open-air → fail; J5b: card chỉ hỗ trợ 1 card → fail")
    void threeOpenAirGpusFail() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("mainboard", withSpec(parts.get("mainboard"), 32, "3"));

        EngineReport report = evaluate(replaceQuantity(lines(parts), "gpu", 3), EvaluationContext.Mode.USER);

        assertThat(statusesOf(report, "J5a")).containsExactly("pass");
        assertThat(statusesOf(report, "J5b")).containsExactly("fail");
        assertThat(statusesOf(report, "J5c")).containsExactly("fail");
    }

    @Test
    @DisplayName("J6a: 1 EPYC trên bo 2 socket → fail; J6b: 2 CPU nhưng 1 tản → fail")
    void dualSocketBoardNeedsTwoCpusAndCoolers() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("cpu", withSpec(parts.get("cpu"), 28, "2"));
        parts.put("mainboard", withSpec(parts.get("mainboard"), 31, "2"));

        assertThat(statusesOf(evaluate(parts, EvaluationContext.Mode.USER), "J6a")).containsExactly("fail");

        EngineReport twoCpus = evaluate(replaceQuantity(lines(parts), "cpu", 2), EvaluationContext.Mode.USER);
        assertThat(statusesOf(twoCpus, "J6a")).containsExactly("pass");
        assertThat(statusesOf(twoCpus, "J6b")).containsExactly("fail");
    }

    @Test
    @DisplayName("J8a: tổng = B + 1 ₫ → fail, tổng = B → pass; J8b: 85% → warning")
    void budgetHasNoOverTolerance() {
        List<BuildLine> lines = lines(validDesktop()); // tổng 27.000.000

        EngineReport over = engine.evaluate(lines, new EvaluationContext(new BigDecimal("26999999"), EvaluationContext.Mode.USER, snapshot()));
        EngineReport exact = engine.evaluate(lines, new EvaluationContext(new BigDecimal("27000000"), EvaluationContext.Mode.USER, snapshot()));
        EngineReport under = engine.evaluate(lines, new EvaluationContext(new BigDecimal("31764706"), EvaluationContext.Mode.USER, snapshot()));

        assertThat(statusesOf(over, "J8a")).containsExactly("fail");
        assertThat(over.compatible()).isFalse();
        assertThat(statusesOf(exact, "J8a")).containsExactly("pass");
        assertThat(statusesOf(exact, "J8b")).isEmpty();
        assertThat(statusesOf(under, "J8a")).containsExactly("pass");
        assertThat(statusesOf(under, "J8b")).containsExactly("warning");
        assertThat(under.compatible()).isTrue();
    }

    @Test
    @DisplayName("DUP: 2 dòng PSU → fail; ROLE: SSD đặt vào role ram → fail")
    void duplicateAndWrongRoleLines() {
        List<BuildLine> duplicated = new ArrayList<>(lines(validDesktop()));
        duplicated.add(new BuildLine("psu", part(16, "psu", "PSU 850W", 2_500_000, Map.of(14, "850")), 1));
        assertThat(statusesOf(evaluate(duplicated, EvaluationContext.Mode.USER), "DUP")).containsExactly("fail");

        Map<String, ComponentCandidateDTO> parts = validDesktop();
        List<BuildLine> wrongRole = new ArrayList<>(lines(parts));
        wrongRole.removeIf(line -> line.role().equals("ram"));
        wrongRole.add(new BuildLine("ram", parts.get("ssd"), 1));
        assertThat(statusesOf(evaluate(wrongRole, EvaluationContext.Mode.USER), "ROLE")).containsExactly("fail");
    }

    @Test
    @DisplayName("CORE: thiếu linh kiện bắt buộc → fail với danh sách nhóm thiếu")
    void missingCorePartsFail() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.remove("psu");
        parts.remove("ssd");

        EngineReport report = evaluate(parts, EvaluationContext.Mode.USER);

        assertThat(report.checks()).anyMatch(check -> "CORE".equals(check.getKey()) && "fail".equals(check.getStatus())
                && check.getDetail().contains("SSD") && check.getDetail().contains("nguồn"));
    }

    @Test
    @DisplayName("J4: (162 + 575 + 80) × 1,3 làm tròn lên = 1063 W; nguồn 1000 W → fail; > 1600 W có J4b")
    void psuSizingUsesPeakPower() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("cpu", withSpec(parts.get("cpu"), 2, "162"));
        parts.put("gpu", withSpec(withSpec(parts.get("gpu"), 2, "575"), 50, "1000"));
        parts.put("psu", withSpec(parts.get("psu"), 14, "1000"));

        EngineReport report = evaluate(parts, EvaluationContext.Mode.USER);
        assertThat(report.requiredPsuW()).isEqualTo(1063);
        assertThat(statusesOf(report, "J4")).containsExactly("fail");

        parts.put("gpu", withSpec(parts.get("gpu"), 54, "2"));
        parts.put("mainboard", withSpec(parts.get("mainboard"), 32, "2"));
        EngineReport twoGpus = evaluate(replaceQuantity(lines(parts), "gpu", 2), EvaluationContext.Mode.USER);
        // 2 GPU: max(ceil((162 + 1150 + 80) × 1,35) = 1880, 1000 + 575 = 1575)
        assertThat(twoGpus.requiredPsuW()).isEqualTo(1880);
        assertThat(statusesOf(twoGpus, "J4b")).containsExactly("warning");
    }

    @Test
    @DisplayName("J10: case thiếu Max_GPU_Length chỉ chấp nhận khi không có GPU rời và không có tản khí")
    void caseMissingClearanceSpecs() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("case", withSpec(withSpec(parts.get("case"), 16, null), 17, null));

        assertThat(statusesOf(evaluate(parts, EvaluationContext.Mode.USER), "J10")).containsExactly("unknown");

        parts.remove("gpu");
        parts.remove("cooler");
        parts.put("cpu", withSpec(parts.get("cpu"), 27, "YES"));
        EngineReport report = evaluate(parts, EvaluationContext.Mode.SOLVER);
        assertThat(statusesOf(report, "J10")).isEmpty();
        assertThat(report.compatible()).isTrue();
    }

    @Test
    @DisplayName("J9a/J9b: ghi chú tương thích và giá ESTIMATE → warning, không chặn")
    void notesAndReferencePricesWarn() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("cpu", withSpec(withSpec(parts.get("cpu"), 67, "Cần cập nhật BIOS"), 66, "ESTIMATE"));

        EngineReport report = evaluate(parts, EvaluationContext.Mode.SOLVER);

        assertThat(statusesOf(report, "J9a")).containsExactly("warning");
        assertThat(statusesOf(report, "J9b")).containsExactly("warning");
        assertThat(report.compatible()).isTrue();
        assertThat(report.hasWarnings()).isTrue();
    }

    @Test
    @DisplayName("Luật mới trong snapshot (ví dụ luật 13) được áp dụng mà không đổi code")
    void newRuleFromSnapshotIsApplied() {
        List<RuleDefinition> rules = new ArrayList<>(RULES);
        rules.add(new RuleDefinition(13, "Đầu cấp nguồn GPU ↔ PSU", 4, 13, 6, 13, "LESS_THAN_OR_EQUAL"));
        CompatibilitySnapshot snapshot = snapshot(rules, List.of());
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("gpu", withSpec(parts.get("gpu"), 13, "2"));
        parts.put("psu", withSpec(parts.get("psu"), 13, "1"));

        EngineReport report = engine.evaluate(lines(parts), new EvaluationContext(null, EvaluationContext.Mode.USER, snapshot));

        assertThat(statusesOf(report, "R13")).containsExactly("fail");
    }

    @Test
    @DisplayName("Thứ tự check: fail, warning, unknown, pass")
    void checksAreOrderedBySeverity() {
        Map<String, ComponentCandidateDTO> parts = validDesktop();
        parts.put("mainboard", withSpec(withSpec(parts.get("mainboard"), 1, "LGA1700"), 32, null));
        parts.put("cpu", withSpec(parts.get("cpu"), 66, "ESTIMATE"));

        List<String> statuses = evaluate(parts, EvaluationContext.Mode.USER).checks().stream()
                .map(CompatibilityCheckResponse::getStatus).toList();

        assertThat(statuses).containsSubsequence("fail", "warning", "unknown", "pass");
        assertThat(statuses.indexOf("pass")).isGreaterThan(statuses.lastIndexOf("unknown"));
        assertThat(statuses.lastIndexOf("fail")).isLessThan(statuses.indexOf("warning"));
    }

    private static List<BuildLine> replaceQuantity(List<BuildLine> lines, String role, int quantity) {
        return lines.stream()
                .map(line -> line.role().equals(role) ? new BuildLine(role, line.part(), quantity) : line)
                .toList();
    }
}
