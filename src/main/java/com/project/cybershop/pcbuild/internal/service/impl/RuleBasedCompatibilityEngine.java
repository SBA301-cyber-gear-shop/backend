package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.pcbuild.api.dto.response.CapacityResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.RuleDefinition;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.PowerRequirement;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.PsuSizer;
import com.project.cybershop.pcbuild.internal.support.BuildMetrics;
import com.project.cybershop.pcbuild.internal.support.ProfileMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Engine tương thích dựa trên luật dữ liệu R1–R12 (bảng compatibility_rules) và luật hệ thống J1–J10 (DD 7.3).
 * Không inject repository hay CatalogPublicApi: mọi dữ liệu đến từ EvaluationContext.snapshot().
 */
@Slf4j
@Service
public class RuleBasedCompatibilityEngine implements CompatibilityEngine {

    // Id thông số (DD 4.2, khớp docs/Metadata.sql)
    private static final int SOCKET = 1;
    private static final int TDP_POWER = 2;
    private static final int MEMORY_TYPE = 4;
    private static final int RAM_SLOTS = 5;
    private static final int MAX_RAM_GB = 6;
    private static final int M2_SLOTS = 8;
    private static final int CAPACITY_GB = 9;
    private static final int WATTAGE = 14;
    private static final int MAX_GPU_LENGTH_MM = 16;
    private static final int MAX_COOLER_HEIGHT_MM = 17;
    private static final int HEIGHT_MM = 19;
    private static final int COOLER_TYPE = 20;
    private static final int HAS_IGPU = 26;
    private static final int INCLUDES_COOLER = 27;
    private static final int CHIPSET = 30;
    private static final int CPU_SOCKET_COUNT = 31;
    private static final int PCIE_X16_SLOTS = 32;
    private static final int MODULES_COUNT = 34;
    private static final int M2_SLOTS_USED = 40;
    private static final int REC_PSU_W = 50;
    private static final int GPU_COOLER_DESIGN = 53;
    private static final int MAX_GPU_QTY = 54;
    private static final int PRICE_STATUS = 66;
    private static final int COMPAT_NOTE = 67;

    private static final int CATEGORY_CASE = 7;
    private static final int CATEGORY_COOLER = 8;

    private static final List<String> CORE_ROLES = List.of("cpu", "mainboard", "ram", "ssd", "psu", "case");
    // ram chỉ 1 dòng (1 SKU, dùng quantity); ssd, gpu được nhiều dòng
    private static final List<String> SINGLE_LINE_ROLES = List.of("cpu", "mainboard", "ram", "psu", "case", "cooler");
    private static final Map<String, String> ROLE_LABELS = Map.of(
            "cpu", "CPU", "mainboard", "bo mạch chủ", "ram", "RAM", "gpu", "card đồ họa",
            "ssd", "SSD", "psu", "nguồn", "case", "vỏ case", "cooler", "tản nhiệt");
    private static final Set<String> REFERENCE_PRICE_STATUSES = Set.of("ESTIMATE", "BUILD_ONLY", "PREORDER", "SNIPPET");
    private static final Set<Integer> SP5_MODULES_PER_SOCKET = Set.of(1, 2, 4, 6, 8, 10, 12);
    private static final Set<Integer> WRX90_MODULES = Set.of(4, 8);

    private static final BigDecimal BUDGET_WINDOW_MIN = new BigDecimal("0.90"); // [ĐÃ CHỐT] cận dưới cửa sổ ngân sách
    private static final int PSU_HIGH_WATTAGE_W = 1600;

    // PsuSizer thuần bộ nhớ (công thức nguồn dùng chung với solver), không phải dependency truy cập dữ liệu
    private final PsuSizer psuSizer;

    @Autowired
    public RuleBasedCompatibilityEngine(PsuSizer psuSizer) {
        this.psuSizer = psuSizer;
    }

    public RuleBasedCompatibilityEngine() {
        this(new PsuSizerImpl());
    }

    @Override
    public EngineReport evaluate(List<BuildLine> lines, EvaluationContext ctx) {
        return new Evaluation(lines, ctx).run();
    }

    /** Trạng thái của một lần đánh giá; tách khỏi engine để engine không giữ state giữa các request. */
    private final class Evaluation {
        private final List<BuildLine> lines;
        private final EvaluationContext ctx;
        private final CompatibilitySnapshot snapshot;
        private final Map<BuildLine, ParsedSpecs> parsedByLine = new IdentityHashMap<>();
        private final List<CompatibilityCheckResponse> checks = new ArrayList<>();
        private int requiredPsuW;

        private Evaluation(List<BuildLine> lines, EvaluationContext ctx) {
            this.lines = lines.stream()
                    .filter(line -> line != null && line.part() != null && line.quantity() > 0)
                    .map(line -> new BuildLine(normalizeRole(line.role()), line.part(), line.quantity()))
                    .toList();
            this.ctx = ctx;
            this.snapshot = ctx.snapshot();
        }

        private EngineReport run() {
            checkCoreParts();
            checkRoles();
            checkDuplicates();
            checkRequiredSpecs();
            checkDataRules();
            checkDisplayOutput();
            checkCpuCooler();
            checkMemoryAndStorage();
            checkPower();
            checkGpuCount();
            checkCpuSockets();
            checkMemoryChannels();
            checkBudget();
            checkNotesAndPrices();
            checkProfile();
            return EngineReport.of(checks, requiredPsuW, capacity(), total());
        }

        // P:* (DD 7.4): lệch hồ sơ chỉ là warning, khách tự đổi linh kiện có quyền chọn khác hồ sơ.
        // Mode SOLVER không sinh vì pool và bước lọc của solver đã bảo đảm các mục này.
        private void checkProfile() {
            ValidatedProfile profile = ctx.profile();
            if (profile == null || ctx.mode() != EvaluationContext.Mode.USER) return;
            for (ProfileConstraintDto constraint : profile.constraints()) {
                for (BuildLine line : linesOf(constraint.getRole())) {
                    if (!ProfileMatcher.matches(constraint, line.part(), snapshot)) {
                        add("P:" + constraint.getId(), "Yêu cầu theo nhu cầu", "warning", "Không đạt yêu cầu '"
                                + reasonOf(constraint.getReason(), constraint.getLabel()) + "': " + name(line)
                                + " chưa đạt " + constraint.getLabel());
                    }
                }
            }
            Map<MetricKind, Integer> metrics = BuildMetrics.of(lines, snapshot);
            for (ProfileMetricDto metric : profile.metrics()) {
                if (!ProfileMatcher.matches(metric, metrics)) {
                    Integer actual = MetricKind.parse(metric.getMetric()).map(metrics::get).orElse(0);
                    add("P:" + metric.getId(), "Yêu cầu theo nhu cầu", "warning", "Không đạt yêu cầu '"
                            + reasonOf(metric.getReason(), metric.getLabel()) + "': " + metric.getLabel() + " (hiện " + actual + ")");
                }
            }
            for (ProfileRoleRefDto required : profile.profile().getRequiredRoles()) {
                if (quantity(required.getRole()) == 0) {
                    add("P:" + required.getRole(), "Yêu cầu theo nhu cầu", "warning", "Thiếu " + roleLabel(required.getRole())
                            + " theo nhu cầu" + (required.getReason() != null ? ": " + required.getReason() : ""));
                }
            }
        }

        // ---------- Kiểm tra cốt lõi ----------

        private void checkCoreParts() {
            List<String> missing = CORE_ROLES.stream().filter(role -> quantity(role) < 1).toList();
            if (missing.isEmpty()) {
                add("CORE", "Linh kiện cốt lõi", "pass", "Đã chọn đủ các nhóm linh kiện cần thiết.");
            } else {
                add("CORE", "Linh kiện cốt lõi", "fail", "Thiếu linh kiện bắt buộc: " + missing.stream()
                        .map(RuleBasedCompatibilityEngine::roleLabel).collect(Collectors.joining(", ")));
            }
        }

        private void checkRoles() {
            for (BuildLine line : lines) {
                String slug = line.part().getCategorySlug();
                if (slug != null && !slug.trim().equalsIgnoreCase(line.role())) {
                    add("ROLE", "Đúng nhóm linh kiện", "fail",
                            "Linh kiện " + name(line) + " không thuộc nhóm " + roleLabel(line.role()));
                }
            }
        }

        private void checkDuplicates() {
            for (String role : SINGLE_LINE_ROLES) {
                if (linesOf(role).size() > 1) {
                    add("DUP", "Trùng nhóm linh kiện", "fail",
                            "Chỉ được chọn một loại " + roleLabel(role) + " (dùng số lượng cho nhiều đơn vị)");
                }
            }
        }

        // J10: thiếu thông số is_compatibility (DD 4.3) kèm hai ngoại lệ
        private void checkRequiredSpecs() {
            boolean hasGpu = quantity("gpu") > 0;
            boolean hasAirCooler = linesOf("cooler").stream().anyMatch(line -> !isLiquid(line));
            for (BuildLine line : lines) {
                Integer categoryId = line.part().getCategoryId();
                ParsedSpecs specs = parsed(line);
                List<Integer> missing = snapshot.requiredSpecIds(categoryId).stream()
                        .filter(specId -> !specs.has(specId))
                        .filter(specId -> !isAcceptedMissingSpec(categoryId, specId, line, hasGpu, hasAirCooler))
                        .sorted()
                        .toList();
                if (!missing.isEmpty()) {
                    add("J10", "Đủ thông số kỹ thuật", missingStatus(), "Linh kiện " + name(line)
                            + " chưa đủ thông số để kiểm tra " + specNames(missing));
                }
            }
        }

        private boolean isAcceptedMissingSpec(Integer categoryId, int specId, BuildLine line,
                                              boolean hasGpu, boolean hasAirCooler) {
            if (categoryId == null) return false;
            if (categoryId == CATEGORY_CASE && (specId == MAX_GPU_LENGTH_MM || specId == MAX_COOLER_HEIGHT_MM)) {
                return !hasGpu && !hasAirCooler;
            }
            return categoryId == CATEGORY_COOLER && specId == HEIGHT_MM && isLiquid(line);
        }

        // ---------- Luật dữ liệu R1..Rn ----------

        private void checkDataRules() {
            for (RuleDefinition rule : snapshot.rules()) {
                List<BuildLine> firstSide = linesOfCategory(rule.category1Id());
                List<BuildLine> secondSide = linesOfCategory(rule.category2Id());
                for (BuildLine first : firstSide) {
                    for (BuildLine second : secondSide) {
                        if (first != second && !isSkippedForLiquidCooler(rule, first, second)) {
                            evaluateRule(rule, first, second);
                        }
                    }
                }
            }
        }

        // J11: luật dùng Height_MM (chiều cao tản khí) không áp cho tản nước
        private boolean isSkippedForLiquidCooler(RuleDefinition rule, BuildLine first, BuildLine second) {
            return (rule.spec1Id() == HEIGHT_MM && isLiquid(first)) || (rule.spec2Id() == HEIGHT_MM && isLiquid(second));
        }

        private void evaluateRule(RuleDefinition rule, BuildLine first, BuildLine second) {
            String key = "R" + rule.id();
            ParsedSpecs firstSpecs = parsed(first);
            ParsedSpecs secondSpecs = parsed(second);
            String firstValue = firstSpecs.raw().get(rule.spec1Id());
            String secondValue = secondSpecs.raw().get(rule.spec2Id());

            if (firstValue == null || secondValue == null) {
                List<String> missing = new ArrayList<>();
                if (firstValue == null) missing.add(snapshot.specName(rule.spec1Id()) + " của " + name(first));
                if (secondValue == null) missing.add(snapshot.specName(rule.spec2Id()) + " của " + name(second));
                add(key, rule.ruleName(), missingStatus(), "Thiếu thông số " + String.join(", ", missing));
                return;
            }

            Boolean passed = compare(rule, firstSpecs, secondSpecs, firstValue, secondValue);
            String expression = snapshot.display(rule.spec1Id(), firstValue) + " " + operatorSymbol(rule.operator())
                    + " " + snapshot.display(rule.spec2Id(), secondValue);
            if (passed == null) {
                if (ctx.mode() == EvaluationContext.Mode.USER) {
                    log.warn("Thông số không so sánh được: rule={}, variant {} spec {}='{}', variant {} spec {}='{}'",
                            rule.id(), first.part().getVariantId(), rule.spec1Id(), firstValue,
                            second.part().getVariantId(), rule.spec2Id(), secondValue);
                }
                add(key, rule.ruleName(), missingStatus(), "Không đọc được giá trị để so sánh (" + expression + ")");
            } else if (passed) {
                add(key, rule.ruleName(), "pass", "Tương thích (" + expression + ")");
            } else {
                add(key, rule.ruleName(), "fail", "Không tương thích: " + name(first) + " và " + name(second)
                        + " không thỏa (" + expression + ")");
            }
        }

        /** true/false = kết quả so sánh; null = không so sánh được (giá trị số bẩn hoặc toán tử lạ). */
        private Boolean compare(RuleDefinition rule, ParsedSpecs firstSpecs, ParsedSpecs secondSpecs,
                                String firstValue, String secondValue) {
            String operator = rule.operator() != null ? rule.operator().trim().toUpperCase(Locale.ROOT) : "";
            switch (operator) {
                case "EQUALS":
                    return firstValue.equalsIgnoreCase(secondValue);
                case "INCLUDES":
                    // Tách vế danh sách thành tập code rồi kiểm phần tử vế còn lại (thay cho contains hai chiều)
                    return secondSpecs.lists().get(rule.spec2Id()).contains(firstValue.toUpperCase(Locale.ROOT))
                            || firstSpecs.lists().get(rule.spec1Id()).contains(secondValue.toUpperCase(Locale.ROOT));
                case "LESS_THAN_OR_EQUAL":
                case "GREATER_THAN_OR_EQUAL": {
                    // Số đã parse sẵn khi dựng snapshot; "65/88" không có trong ints nên ra null (unknown/fail)
                    Integer first = firstSpecs.ints().get(rule.spec1Id());
                    Integer second = secondSpecs.ints().get(rule.spec2Id());
                    if (first == null || second == null) return null;
                    int comparison = first.compareTo(second);
                    return "LESS_THAN_OR_EQUAL".equals(operator) ? comparison <= 0 : comparison >= 0;
                }
                default:
                    log.warn("Toán tử luật không hỗ trợ: rule={}, operator={}", rule.id(), rule.operator());
                    return null;
            }
        }

        // ---------- Luật hệ thống J1..J9 ----------

        // J1: không GPU rời thì CPU phải có iGPU
        private void checkDisplayOutput() {
            BuildLine cpu = first("cpu");
            if (cpu == null) return;
            if (quantity("gpu") > 0) {
                add("J1", "Có hình ảnh", "pass", "Đã có card đồ họa rời.");
                return;
            }
            String integratedGraphics = parsed(cpu).raw().get(HAS_IGPU);
            if ("YES".equalsIgnoreCase(integratedGraphics)) {
                add("J1", "Có hình ảnh", "pass", "CPU " + name(cpu) + " có đồ họa tích hợp.");
            } else if ("NO".equalsIgnoreCase(integratedGraphics)) {
                add("J1", "Có hình ảnh", "fail",
                        "CPU " + name(cpu) + " không có card đồ họa tích hợp, cần thêm card đồ họa rời");
            } else {
                add("J1", "Có hình ảnh", missingStatus(),
                        "Chưa xác minh CPU " + name(cpu) + " có đồ họa tích hợp hay không");
            }
        }

        // J2: CPU không kèm tản thì phải có tản rời
        private void checkCpuCooler() {
            BuildLine cpu = first("cpu");
            if (cpu == null) return;
            if (quantity("cooler") > 0) {
                add("J2", "Có tản nhiệt CPU", "pass", "Đã chọn tản nhiệt rời.");
                return;
            }
            String includesCooler = parsed(cpu).raw().get(INCLUDES_COOLER);
            if ("YES".equalsIgnoreCase(includesCooler)) {
                add("J2", "Có tản nhiệt CPU", "pass", "CPU " + name(cpu) + " kèm tản theo hộp.");
            } else if ("NO".equalsIgnoreCase(includesCooler)) {
                add("J2", "Có tản nhiệt CPU", "fail", "CPU " + name(cpu) + " không kèm tản, cần chọn tản nhiệt");
            } else {
                add("J2", "Có tản nhiệt CPU", missingStatus(),
                        "Chưa xác minh CPU " + name(cpu) + " có kèm tản hay không");
            }
        }

        // J3a–J3d: khe RAM, RAM tối đa, khe M.2 tính theo số lượng
        private void checkMemoryAndStorage() {
            BuildLine mainboard = first("mainboard");
            BuildLine ram = first("ram");
            if (mainboard == null) return;
            ParsedSpecs board = parsed(mainboard);

            if (ram != null) {
                ParsedSpecs memory = parsed(ram);
                Integer modules = multiply(memory.ints().get(MODULES_COUNT), ram.quantity());
                Integer slots = board.ints().get(RAM_SLOTS);
                if (modules == null || slots == null) {
                    add("J3a", "Số khe cắm RAM", missingStatus(), "Thiếu thông số số thanh RAM hoặc số khe của bo mạch");
                } else if (modules <= slots) {
                    add("J3a", "Số khe cắm RAM", "pass", "Dùng " + modules + "/" + slots + " khe RAM.");
                } else {
                    add("J3a", "Số khe cắm RAM", "fail", "Cần " + modules + " khe RAM, bo mạch chỉ có " + slots);
                }

                Integer totalGb = multiply(memory.ints().get(CAPACITY_GB), ram.quantity());
                Integer maxGb = board.ints().get(MAX_RAM_GB);
                if (totalGb == null || maxGb == null) {
                    add("J3b", "Dung lượng RAM tối đa", missingStatus(), "Thiếu thông số dung lượng RAM hoặc RAM tối đa của bo");
                } else if (totalGb <= maxGb) {
                    add("J3b", "Dung lượng RAM tối đa", "pass", "Tổng RAM " + totalGb + " GB / tối đa " + maxGb + " GB.");
                } else {
                    add("J3b", "Dung lượng RAM tối đa", "fail",
                            "Tổng RAM " + totalGb + " GB vượt mức tối đa " + maxGb + " GB của bo");
                }

                if (modules != null && modules >= 4 && "DDR5".equalsIgnoreCase(memory.raw().get(MEMORY_TYPE))) {
                    add("J3d", "4 thanh DDR5", "warning", "Dùng " + modules + " thanh DDR5 có thể phải giảm tốc độ RAM");
                }
            }

            List<BuildLine> ssds = linesOf("ssd");
            if (!ssds.isEmpty()) {
                Integer used = 0;
                for (BuildLine ssd : ssds) {
                    Integer slotsUsed = multiply(parsed(ssd).ints().get(M2_SLOTS_USED), ssd.quantity());
                    used = slotsUsed != null && used != null ? used + slotsUsed : null;
                }
                Integer slots = board.ints().get(M2_SLOTS);
                if (used == null || slots == null) {
                    add("J3c", "Số khe M.2", missingStatus(), "Thiếu thông số khe M.2 của SSD hoặc bo mạch");
                } else if (used <= slots) {
                    add("J3c", "Số khe M.2", "pass", "Dùng " + used + "/" + slots + " khe M.2.");
                } else {
                    add("J3c", "Số khe M.2", "fail", "Cần " + used + " khe M.2, bo mạch có " + slots);
                }
            }
        }

        // J4, J4b: công suất nguồn theo công thức PsuSizer (DD 6.5)
        private void checkPower() {
            PowerRequirement estimate = psuSizer.requiredW(lines, snapshot);
            if (estimate != null) {
                requiredPsuW = estimate.requiredW();
            }
            BuildLine psu = first("psu");
            if (psu == null || first("cpu") == null) return;

            Integer wattage = parsed(psu).ints().get(WATTAGE);
            if (estimate == null || wattage == null) {
                add("J4", "Công suất Nguồn (PSU)", missingStatus(),
                        "Thiếu TDP, công suất khuyến nghị của card hoặc công suất nguồn, chưa thể kiểm tra");
                return;
            }
            String detail = "Nguồn " + wattage + "W, mức an toàn " + estimate.requiredW() + "W (tải ước tính "
                    + estimate.loadW() + "W)";
            if (wattage >= estimate.requiredW()) {
                add("J4", "Công suất Nguồn (PSU)", "pass", detail + ".");
            } else {
                add("J4", "Công suất Nguồn (PSU)", "fail", "Nguồn " + wattage + "W thấp hơn mức an toàn "
                        + estimate.requiredW() + "W (tải ước tính " + estimate.loadW() + "W)");
            }
            if (estimate.requiredW() > PSU_HIGH_WATTAGE_W) {
                add("J4b", "Nguồn công suất rất lớn", "warning",
                        "Hệ thống cần nguồn trên 1600 W, cần ổ điện 220–240 V riêng, khuyến nghị UPS");
            }
        }


        // J5a–J5c: số card đồ họa
        private void checkGpuCount() {
            List<BuildLine> gpus = linesOf("gpu");
            int gpuCount = quantity("gpu");
            if (gpuCount == 0) return;

            BuildLine mainboard = first("mainboard");
            if (mainboard != null) {
                Integer slots = parsed(mainboard).ints().get(PCIE_X16_SLOTS);
                if (slots == null) {
                    add("J5a", "Số khe PCIe x16", missingStatus(), "Thiếu thông số số khe PCIe x16 của bo mạch");
                } else if (gpuCount <= slots) {
                    add("J5a", "Số khe PCIe x16", "pass", "Dùng " + gpuCount + "/" + slots + " khe PCIe x16.");
                } else {
                    add("J5a", "Số khe PCIe x16", "fail",
                            "Bo mạch chỉ có " + slots + " khe PCIe x16, đang chọn " + gpuCount + " card");
                }
            }

            if (gpuCount >= 2) {
                for (BuildLine gpu : gpus) {
                    Integer maxQuantity = parsed(gpu).ints().getOrDefault(MAX_GPU_QTY, 1); // thiếu: coi là 1
                    if (gpuCount > maxQuantity) {
                        add("J5b", "Số card đồ họa hỗ trợ", "fail",
                                "Card " + name(gpu) + " không hỗ trợ chạy " + gpuCount + " card cùng lúc");
                    }
                }
            }

            if (gpuCount >= 3) {
                for (BuildLine gpu : gpus) {
                    String design = parsed(gpu).raw().get(GPU_COOLER_DESIGN);
                    if (design == null) {
                        add("J5c", "Card tản kiểu blower", missingStatus(), "Chưa rõ kiểu tản của card " + name(gpu));
                    } else if (!"BLOWER".equalsIgnoreCase(design)) {
                        add("J5c", "Card tản kiểu blower", "fail", "Từ 3 card trở lên phải dùng card tản kiểu blower ("
                                + name(gpu) + " là " + snapshot.display(GPU_COOLER_DESIGN, design) + ")");
                    }
                }
            }
        }

        // J6a, J6b: số CPU khớp số socket, số tản khớp số CPU
        private void checkCpuSockets() {
            int cpuCount = quantity("cpu");
            BuildLine mainboard = first("mainboard");
            if (cpuCount > 0 && mainboard != null) {
                Integer sockets = parsed(mainboard).ints().get(CPU_SOCKET_COUNT);
                if (sockets == null) {
                    add("J6a", "Số CPU theo socket", missingStatus(), "Thiếu thông số số socket CPU của bo mạch");
                } else if (cpuCount == sockets) {
                    add("J6a", "Số CPU theo socket", "pass", "Bo mạch " + sockets + " socket, chọn " + cpuCount + " CPU.");
                } else {
                    add("J6a", "Số CPU theo socket", "fail",
                            "Bo mạch có " + sockets + " socket, đang chọn " + cpuCount + " CPU");
                }
            }
            int coolerCount = quantity("cooler");
            if (cpuCount > 0 && coolerCount > 0 && coolerCount != cpuCount) {
                add("J6b", "Tản nhiệt theo số CPU", "fail", "Cần " + cpuCount + " tản nhiệt cho " + cpuCount
                        + " CPU (đang chọn " + coolerCount + ")");
            }
        }

        // J7: cân bằng kênh RDIMM trên WRX90 / SP5 (warning, không chặn)
        private void checkMemoryChannels() {
            BuildLine mainboard = first("mainboard");
            BuildLine ram = first("ram");
            if (mainboard == null || ram == null) return;
            ParsedSpecs board = parsed(mainboard);
            Integer modules = multiply(parsed(ram).ints().get(MODULES_COUNT), ram.quantity());
            if (modules == null) return;

            boolean balanced = true;
            if ("WRX90".equalsIgnoreCase(board.raw().get(CHIPSET))) {
                balanced = WRX90_MODULES.contains(modules);
            } else if ("SP5".equalsIgnoreCase(board.raw().get(SOCKET))) {
                int sockets = board.ints().getOrDefault(CPU_SOCKET_COUNT, 1);
                balanced = modules % sockets == 0 && SP5_MODULES_PER_SOCKET.contains(modules / sockets);
            }
            if (!balanced) {
                add("J7", "Cân bằng kênh bộ nhớ", "warning",
                        "Số thanh RDIMM " + modules + " không cân bằng kênh bộ nhớ, hiệu năng giảm");
            }
        }

        // J8a, J8b: tổng ≤ B (không có biên vượt), cảnh báo khi dưới 90%
        private void checkBudget() {
            BigDecimal budget = ctx.budget();
            if (budget == null || budget.signum() <= 0) return;
            BigDecimal total = total();
            if (total.compareTo(budget) <= 0) {
                add("J8a", "Ngân sách", "pass", "Tổng " + vnd(total) + " / ngân sách " + vnd(budget) + ".");
            } else {
                add("J8a", "Ngân sách", "fail", "Tổng " + vnd(total) + " vượt ngân sách " + vnd(budget)
                        + " (vượt " + vnd(total.subtract(budget)) + ")");
            }
            if (total.compareTo(budget.multiply(BUDGET_WINDOW_MIN)) < 0) {
                BigDecimal percent = total.multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP);
                add("J8b", "Độ sát ngân sách", "warning", "Tổng " + vnd(total) + " mới đạt " + percent
                        + "% ngân sách, còn dư " + vnd(budget.subtract(total)));
            }
        }

        // J9a, J9b: ghi chú tương thích và giá tham khảo
        private void checkNotesAndPrices() {
            for (BuildLine line : lines) {
                ParsedSpecs specs = parsed(line);
                String note = specs.raw().get(COMPAT_NOTE);
                if (note != null) {
                    add("J9a", "Ghi chú tương thích", "warning", name(line) + ": " + note);
                }
                String priceStatus = specs.raw().get(PRICE_STATUS);
                if (priceStatus != null && REFERENCE_PRICE_STATUSES.contains(priceStatus.toUpperCase(Locale.ROOT))) {
                    add("J9b", "Giá tham khảo", "warning", "Giá " + name(line) + " là giá tham khảo ("
                            + priceStatus + "), vui lòng liên hệ xác nhận");
                }
            }
        }

        // ---------- Kết quả phụ ----------

        private CapacityResponse capacity() {
            BuildLine mainboard = first("mainboard");
            BuildLine ram = first("ram");
            ParsedSpecs board = mainboard != null ? parsed(mainboard) : null;
            ParsedSpecs memory = ram != null ? parsed(ram) : null;
            Integer m2Used = 0;
            for (BuildLine ssd : linesOf("ssd")) {
                Integer slotsUsed = multiply(parsed(ssd).ints().get(M2_SLOTS_USED), ssd.quantity());
                m2Used = slotsUsed != null && m2Used != null ? m2Used + slotsUsed : null;
            }
            return CapacityResponse.builder()
                    .ramSlotsUsed(memory != null ? multiply(memory.ints().get(MODULES_COUNT), ram.quantity()) : null)
                    .ramSlotsMax(board != null ? board.ints().get(RAM_SLOTS) : null)
                    .ramGb(memory != null ? multiply(memory.ints().get(CAPACITY_GB), ram.quantity()) : null)
                    .ramMaxGb(board != null ? board.ints().get(MAX_RAM_GB) : null)
                    .m2Used(m2Used)
                    .m2Max(board != null ? board.ints().get(M2_SLOTS) : null)
                    .gpuCount(quantity("gpu"))
                    .gpuMax(board != null ? board.ints().get(PCIE_X16_SLOTS) : null)
                    .build();
        }

        private BigDecimal total() {
            BigDecimal total = BigDecimal.ZERO;
            for (BuildLine line : lines) {
                if (line.part().getPrice() != null) {
                    total = total.add(line.part().getPrice().multiply(BigDecimal.valueOf(line.quantity())));
                }
            }
            return total;
        }

        // ---------- Tiện ích ----------

        private void add(String key, String label, String status, String detail) {
            checks.add(CompatibilityCheckResponse.of(key, label, status, detail));
        }

        private String missingStatus() {
            return ctx.mode() == EvaluationContext.Mode.SOLVER ? "fail" : "unknown";
        }

        private ParsedSpecs parsed(BuildLine line) {
            return parsedByLine.computeIfAbsent(line, key -> snapshot.parsed(key.part()));
        }

        private List<BuildLine> linesOf(String role) {
            return lines.stream().filter(line -> role.equals(line.role())).toList();
        }

        private List<BuildLine> linesOfCategory(int categoryId) {
            return lines.stream()
                    .filter(line -> line.part().getCategoryId() != null && line.part().getCategoryId() == categoryId)
                    .toList();
        }

        private BuildLine first(String role) {
            return lines.stream().filter(line -> role.equals(line.role())).findFirst().orElse(null);
        }

        private int quantity(String role) {
            return linesOf(role).stream().mapToInt(BuildLine::quantity).sum();
        }

        private boolean isLiquid(BuildLine line) {
            return "Liquid".equalsIgnoreCase(parsed(line).raw().get(COOLER_TYPE));
        }

        private String specNames(List<Integer> specIds) {
            return specIds.stream().map(snapshot::specName).collect(Collectors.joining(", "));
        }
    }

    private static String reasonOf(String reason, String label) {
        return reason != null && !reason.isBlank() ? reason : label;
    }

    private static String normalizeRole(String role) {
        return role != null ? role.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static String roleLabel(String role) {
        return ROLE_LABELS.getOrDefault(role, role);
    }

    private static String name(BuildLine line) {
        String productName = line.part().getProductName();
        return productName != null ? productName : "#" + line.part().getVariantId();
    }

    private static Integer multiply(Integer value, int quantity) {
        return value != null ? value * quantity : null;
    }

    private static String operatorSymbol(String operator) {
        if (operator == null) return "?";
        switch (operator.trim().toUpperCase(Locale.ROOT)) {
            case "EQUALS":
                return "=";
            case "INCLUDES":
                return "∈";
            case "LESS_THAN_OR_EQUAL":
                return "≤";
            case "GREATER_THAN_OR_EQUAL":
                return "≥";
            default:
                return operator;
        }
    }

    private static String vnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount.setScale(0, RoundingMode.HALF_UP)) + " ₫";
    }
}
