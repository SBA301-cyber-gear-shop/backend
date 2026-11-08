package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.BuildCheck;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.model.RuleViolation;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.support.BuildIds;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bước 6 của docx: chạy mọi luật trong compatibility_rules, cộng R7 (công suất nguồn) và luật khuôn,
 * rồi tách INCOMPATIBLE (xung đột phần cứng) với OVER_BUDGET (chỉ vượt tiền).
 */
@Slf4j
@Service
public class CompatibilityEngineImpl implements CompatibilityEngine {

    /** R7: nguồn ≥ (TDP CPU + TDP GPU) × 1,3; viết dạng phân số để tính bằng số nguyên. */
    private static final long PSU_HEADROOM_NUMERATOR = 13;
    private static final long PSU_HEADROOM_DENOMINATOR = 10;

    private static final String EQUALS = "EQUALS";
    private static final String INCLUDES = "INCLUDES";
    private static final String LESS_THAN_OR_EQUAL = "LESS_THAN_OR_EQUAL";
    private static final String GREATER_THAN_OR_EQUAL = "GREATER_THAN_OR_EQUAL";

    @Override
    public List<RuleViolation> checkRules(List<BuildLine> lines, List<CompatibilityRule> rules) {
        List<RuleViolation> violations = new ArrayList<>();
        for (CompatibilityRule rule : rules) {
            for (BuildLine first : lines) {
                if (!Objects.equals(first.categoryId(), rule.getCategory1Id())) {
                    continue;
                }
                for (BuildLine second : lines) {
                    if (first == second || !Objects.equals(second.categoryId(), rule.getCategory2Id())) {
                        continue;
                    }
                    RuleViolation violation = evaluate(rule, first, second);
                    if (violation != null) {
                        violations.add(violation);
                    }
                }
            }
        }
        RuleViolation powerViolation = checkPsuPower(lines);
        if (powerViolation != null) {
            violations.add(powerViolation);
        }
        return violations;
    }

    @Override
    public int maxQuantity(SlotMeta slot, ComponentCandidateDTO part, List<BuildLine> others,
                           List<CompatibilityRule> rules) {
        int max = codedMaxQuantity(slot, part, others);
        // Giới hạn tự suy từ luật nhân số lượng: perUnit × qty ≤ capacity → qty ≤ ⌊capacity / perUnit⌋
        for (CompatibilityRule rule : rules) {
            if (!Objects.equals(rule.getCategory1Id(), slot.getCategoryId())
                    || !BuildIds.ADDITIVE_SPEC_IDS.contains(rule.getSpec1Id())
                    || !LESS_THAN_OR_EQUAL.equals(rule.getOperator())) {
                continue;
            }
            Long perUnit = number(part.getSpecById(rule.getSpec1Id()));
            if (perUnit == null || perUnit <= 0) {
                continue;
            }
            for (BuildLine other : others) {
                if (!Objects.equals(other.categoryId(), rule.getCategory2Id())) {
                    continue;
                }
                Long capacity = number(other.part().getSpecById(rule.getSpec2Id()));
                if (capacity != null) {
                    max = (int) Math.min(max, capacity / perUnit);
                }
            }
        }
        return Math.max(max, 0);
    }

    @Override
    public boolean triggersRequiredSlot(SlotMeta slot, ComponentCandidateDTO part) {
        if (slot.getRequiredIfCategoryId() == null || !Objects.equals(part.getCategoryId(), slot.getRequiredIfCategoryId())) {
            return false;
        }
        String value = part.getSpecById(slot.getRequiredIfSpecId());
        // D3 đã chốt: UNKNOWN hoặc thiếu thông số cũng coi là thỏa điều kiện (CPU không chắc có iGPU thì cần GPU)
        return isBlank(value)
                || value.trim().equalsIgnoreCase(slot.getRequiredIfValue())
                || value.trim().equalsIgnoreCase(BuildIds.VALUE_UNKNOWN);
    }

    @Override
    public BuildCheck checkBuild(long budget, List<BuildLine> lines, BuildContext context) {
        List<RuleViolation> violations = new ArrayList<>(checkTemplate(lines, context));
        violations.addAll(checkRules(lines, context.rules()));
        long total = lines.stream().mapToLong(BuildLine::lineTotal).sum();
        long difference = total - budget;
        BuildStatus status;
        if (!violations.isEmpty()) {
            status = BuildStatus.INCOMPATIBLE;
        } else if (total > budget) {
            status = BuildStatus.OVER_BUDGET;
        } else {
            status = BuildStatus.VALID;
        }
        log.info("engine status={} total={} budget={} violations={}", status, total, budget,
                violations.stream().map(RuleViolation::rule).toList());
        return new BuildCheck(status, total, difference, List.copyOf(violations));
    }

    // =========================================================================
    // LUẬT DB
    // =========================================================================

    private RuleViolation evaluate(CompatibilityRule rule, BuildLine first, BuildLine second) {
        if (isSkippedByCoolerType(rule, first, second)) {
            return null;
        }
        String firstValue = first.part().getSpecById(rule.getSpec1Id());
        String secondValue = second.part().getSpecById(rule.getSpec2Id());
        if (isBlank(firstValue) || isBlank(secondValue)) {
            return violation(rule, first.displayName() + " hoặc " + second.displayName() + " thiếu thông số để kiểm tra");
        }
        Boolean passed = switch (rule.getOperator()) {
            case EQUALS -> firstValue.trim().equalsIgnoreCase(secondValue.trim());
            case INCLUDES -> codes(secondValue).contains(normalize(firstValue))
                    || codes(firstValue).contains(normalize(secondValue));
            case LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL -> compareNumbers(rule, first, firstValue, secondValue);
            default -> {
                log.warn("Luật {} có toán tử lạ {}, coi là không qua", rule.getId(), rule.getOperator());
                yield false;
            }
        };
        if (passed == null) {
            return violation(rule, first.displayName() + " hoặc " + second.displayName() + " có thông số không phải số");
        }
        if (passed) {
            return null;
        }
        String firstShown = BuildIds.ADDITIVE_SPEC_IDS.contains(rule.getSpec1Id())
                ? firstValue.trim() + " × " + first.quantity()
                : firstValue.trim();
        return violation(rule, first.displayName() + " (" + firstShown + ") không khớp "
                + second.displayName() + " (" + secondValue.trim() + ")");
    }

    /** null khi một vế không đọc được thành số. */
    private static Boolean compareNumbers(CompatibilityRule rule, BuildLine first, String firstValue, String secondValue) {
        Long left = number(firstValue);
        Long right = number(secondValue);
        if (left == null || right == null) {
            return null;
        }
        if (BuildIds.ADDITIVE_SPEC_IDS.contains(rule.getSpec1Id())) {
            left = left * first.quantity();
        }
        return LESS_THAN_OR_EQUAL.equals(rule.getOperator()) ? left <= right : left >= right;
    }

    /** Luật chiều cao tản chỉ áp cho tản khí, luật radiator chỉ áp cho tản nước. */
    private static boolean isSkippedByCoolerType(CompatibilityRule rule, BuildLine first, BuildLine second) {
        BuildLine cooler;
        Integer coolerSpecId;
        if (Objects.equals(first.categoryId(), BuildIds.CATEGORY_COOLER)) {
            cooler = first;
            coolerSpecId = rule.getSpec1Id();
        } else if (Objects.equals(second.categoryId(), BuildIds.CATEGORY_COOLER)) {
            cooler = second;
            coolerSpecId = rule.getSpec2Id();
        } else {
            return false;
        }
        String coolerType = normalize(cooler.part().getSpecById(BuildIds.SPEC_COOLER_TYPE));
        if (Objects.equals(coolerSpecId, BuildIds.SPEC_HEIGHT_MM)) {
            return BuildIds.COOLER_LIQUID.equals(coolerType);
        }
        if (Objects.equals(coolerSpecId, BuildIds.SPEC_RADIATOR_SIZE_MM)) {
            return BuildIds.COOLER_AIR.equals(coolerType);
        }
        return false;
    }

    /** R7 (docx 3.3): chạy khi bộ có cả CPU và PSU. */
    private static RuleViolation checkPsuPower(List<BuildLine> lines) {
        List<BuildLine> psus = linesOf(lines, BuildIds.CATEGORY_PSU);
        List<BuildLine> cpus = linesOf(lines, BuildIds.CATEGORY_CPU);
        if (psus.isEmpty() || cpus.isEmpty()) {
            return null;
        }
        long loadWatt = 0;
        for (BuildLine line : lines) {
            if (Objects.equals(line.categoryId(), BuildIds.CATEGORY_CPU)
                    || Objects.equals(line.categoryId(), BuildIds.CATEGORY_GPU)) {
                Long tdp = number(line.part().getSpecById(BuildIds.SPEC_TDP_POWER));
                if (tdp == null) {
                    return new RuleViolation("PSU_POWER", line.displayName() + " thiếu thông số TDP_Power để tính công suất nguồn");
                }
                loadWatt += tdp * line.quantity();
            }
        }
        long requiredWatt = (loadWatt * PSU_HEADROOM_NUMERATOR + PSU_HEADROOM_DENOMINATOR - 1) / PSU_HEADROOM_DENOMINATOR;
        long wattage = 0;
        for (BuildLine psu : psus) {
            Long value = number(psu.part().getSpecById(BuildIds.SPEC_WATTAGE));
            if (value == null) {
                return new RuleViolation("PSU_POWER", psu.displayName() + " thiếu thông số Wattage");
            }
            wattage += value * psu.quantity();
        }
        if (wattage >= requiredWatt) {
            return null;
        }
        return new RuleViolation("PSU_POWER", "Nguồn " + wattage + "W chưa đủ: CPU + GPU " + loadWatt
                + "W × 1,3 cần tối thiểu " + requiredWatt + "W");
    }

    // =========================================================================
    // LUẬT KHUÔN (build_template_slots)
    // =========================================================================

    private List<RuleViolation> checkTemplate(List<BuildLine> lines, BuildContext context) {
        List<RuleViolation> violations = new ArrayList<>();
        Map<Integer, List<BuildLine>> linesBySlot = new HashMap<>();
        for (BuildLine line : lines) {
            if (line.slot() == null) {
                violations.add(new RuleViolation("NOT_IN_TEMPLATE",
                        line.displayName() + " thuộc loại linh kiện không có trong khuôn bộ PC"));
                continue;
            }
            linesBySlot.computeIfAbsent(line.slot().getSlotId(), key -> new ArrayList<>()).add(line);
        }
        for (SlotMeta slot : context.metadata().getSlots()) {
            List<BuildLine> slotLines = linesBySlot.getOrDefault(slot.getSlotId(), List.of());
            if (slotLines.isEmpty()) {
                RuleViolation missing = missingSlot(slot, lines);
                if (missing != null) {
                    violations.add(missing);
                }
                continue;
            }
            if (slotLines.size() > 1) {
                violations.add(new RuleViolation("DUPLICATE_SLOT",
                        slot.getLabel() + " chỉ được chọn một mẫu, hiện có " + slotLines.size() + " mẫu"));
                continue;
            }
            BuildLine line = slotLines.get(0);
            List<BuildLine> others = lines.stream().filter(other -> other != line).toList();
            int minQty = Math.max(1, slot.getMinQty() != null ? slot.getMinQty() : 1);
            int maxQty = codedMaxQuantity(slot, line.part(), others);
            if (line.quantity() < minQty || line.quantity() > maxQty) {
                violations.add(new RuleViolation("QTY_RANGE", line.displayName() + ": số lượng " + line.quantity()
                        + " nằm ngoài khoảng cho phép " + minQty + "–" + maxQty));
            }
        }
        return violations;
    }

    private RuleViolation missingSlot(SlotMeta slot, List<BuildLine> lines) {
        if (slot.isRequired()) {
            return new RuleViolation("MISSING_SLOT", "Bộ PC thiếu " + slot.getLabel());
        }
        for (BuildLine line : lines) {
            if (triggersRequiredSlot(slot, line.part())) {
                return new RuleViolation("MISSING_SLOT", line.displayName()
                        + " không có đồ họa tích hợp (hoặc chưa rõ) nên bộ PC cần " + slot.getLabel());
            }
        }
        return null;
    }

    /** Giới hạn số lượng do code kiểm (docx 3.1): max_qty của slot, số khe PCIe cho GPU, số socket cho CPU, mỗi CPU một tản. */
    private static int codedMaxQuantity(SlotMeta slot, ComponentCandidateDTO part, List<BuildLine> others) {
        int max = slot.getMaxQty() != null ? slot.getMaxQty() : 1;
        Integer categoryId = slot.getCategoryId();
        if (Objects.equals(categoryId, BuildIds.CATEGORY_GPU)) {
            max = minWithSpec(max, others, BuildIds.CATEGORY_MAINBOARD, BuildIds.SPEC_PCIE_X16_SLOTS);
            Long maxGpuQty = number(part.getSpecById(BuildIds.SPEC_MAX_GPU_QTY));
            if (maxGpuQty != null) {
                max = (int) Math.min(max, maxGpuQty);
            }
        } else if (Objects.equals(categoryId, BuildIds.CATEGORY_CPU)) {
            max = minWithSpec(max, others, BuildIds.CATEGORY_MAINBOARD, BuildIds.SPEC_CPU_SOCKET_COUNT);
        } else if (Objects.equals(categoryId, BuildIds.CATEGORY_COOLER)) {
            List<BuildLine> cpus = linesOf(others, BuildIds.CATEGORY_CPU);
            if (!cpus.isEmpty()) {
                max = Math.min(max, cpus.stream().mapToInt(BuildLine::quantity).sum());
            }
        }
        return max;
    }

    private static int minWithSpec(int max, List<BuildLine> others, int categoryId, int specId) {
        for (BuildLine other : linesOf(others, categoryId)) {
            Long value = number(other.part().getSpecById(specId));
            if (value != null) {
                max = (int) Math.min(max, value);
            }
        }
        return max;
    }

    // =========================================================================
    // TIỆN ÍCH
    // =========================================================================

    private static RuleViolation violation(CompatibilityRule rule, String detail) {
        return new RuleViolation("RULE_" + rule.getId(), rule.getRuleName() + ": " + detail);
    }

    private static List<BuildLine> linesOf(List<BuildLine> lines, int categoryId) {
        return lines.stream().filter(line -> Objects.equals(line.categoryId(), categoryId)).toList();
    }

    /** "AM4, AM5,LGA1700" → {AM4, AM5, LGA1700}. */
    private static Set<String> codes(String value) {
        return Arrays.stream(value.split(","))
                .map(CompatibilityEngineImpl::normalize)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toSet());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static Long number(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
