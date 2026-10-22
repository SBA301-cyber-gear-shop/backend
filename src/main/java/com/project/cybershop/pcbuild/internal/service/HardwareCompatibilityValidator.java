package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HardwareCompatibilityValidator {

    private final CompatibilityRuleRepository ruleRepository;

    public record ValidationReport(
        boolean isValid,
        List<CompatibilityCheckResponse> checks,
        List<String> failureReasons
    ) {}

    public ValidationReport validate(Map<String, ComponentCandidateDTO> parts, BigDecimal budget) {
        List<CompatibilityCheckResponse> checks = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        ComponentCandidateDTO cpu = parts.get("cpu");
        ComponentCandidateDTO mainboard = parts.get("mainboard");
        ComponentCandidateDTO ram = parts.get("ram");
        ComponentCandidateDTO gpu = parts.get("gpu");
        ComponentCandidateDTO ssd = parts.get("ssd");
        ComponentCandidateDTO psu = parts.get("psu");
        ComponentCandidateDTO pcCase = parts.get("case");
        ComponentCandidateDTO cooler = parts.get("cooler");

        // 1. Kiểm tra đủ 6 linh kiện cốt lõi bắt buộc (System Rule 1)
        boolean hasCoreParts = cpu != null && mainboard != null && ram != null 
                             && ssd != null && psu != null && pcCase != null;
        checks.add(new CompatibilityCheckResponse("Linh kiện cốt lõi", 
            hasCoreParts ? "pass" : "fail", 
            hasCoreParts ? "Đã chọn đủ các nhóm linh kiện cần thiết." : "Thiếu linh kiện cốt lõi để khởi động máy!"));
        if (!hasCoreParts) failures.add("Thiếu linh kiện bắt buộc");

        // 2. Map các linh kiện theo categoryId để đối chiếu quy tắc động
        Map<Integer, ComponentCandidateDTO> partsByCategoryId = parts.values().stream()
            .filter(p -> p != null && p.getCategoryId() != null)
            .collect(Collectors.toMap(ComponentCandidateDTO::getCategoryId, p -> p, (a, b) -> a));

        // 3. Thực thi thẩm định ĐỘNG từ bảng compatibility_rules
        List<CompatibilityRule> dynamicRules = ruleRepository.findAll();
        for (CompatibilityRule rule : dynamicRules) {
            ComponentCandidateDTO part1 = partsByCategoryId.get(rule.getCategory1Id());
            ComponentCandidateDTO part2 = partsByCategoryId.get(rule.getCategory2Id());

            // Fallback tra cứu theo slug nếu linh kiện chưa gán categoryId
            if (part1 == null) part1 = findPartByFallbackCategory(parts, rule.getCategory1Id());
            if (part2 == null) part2 = findPartByFallbackCategory(parts, rule.getCategory2Id());

            // Chỉ thẩm định khi cả 2 nhóm linh kiện tham gia quy tắc đều đã được chọn
            if (part1 != null && part2 != null) {
                // Ngoại lệ thực tế: Tản nhiệt nước AIO bỏ qua kiểm tra chiều cao tản khí trong Case
                if (rule.getCategory1Id() == 8 && "Liquid".equalsIgnoreCase(part1.getSpecOrDefault("Cooler_Type", "Air"))) {
                    continue;
                }

                String val1 = getSpecValue(part1, rule.getSpec1Id());
                String val2 = getSpecValue(part2, rule.getSpec2Id());

                boolean rulePassed = evaluateOperator(val1, val2, rule.getOperator());
                String details = rulePassed 
                    ? String.format("Tương thích (%s %s %s)", val1, rule.getOperator(), val2)
                    : String.format("XUNG ĐỘT! %s: Giá trị '%s' không khớp '%s' theo toán tử %s", 
                                    rule.getRuleName(), val1, val2, rule.getOperator());

                checks.add(new CompatibilityCheckResponse(rule.getRuleName(), rulePassed ? "pass" : "fail", details));
                if (!rulePassed) {
                    failures.add(rule.getRuleName() + " không tương thích");
                }
            }
        }

        // 4. Kiểm tra số thanh RAM <= số khe cắm bo mạch chủ (System Rule 2)
        if (ram != null && mainboard != null) {
            int ramQty = ram.getQuantity();
            int mbSlots = Integer.parseInt(mainboard.getSpecOrDefault("RAM_Slots", "4"));
            boolean slotOk = ramQty <= mbSlots;
            if (!slotOk) {
                checks.add(new CompatibilityCheckResponse("Số khe cắm RAM", "fail", 
                    String.format("QUÁ KHE CẮM! Đang chọn %d thanh RAM trong khi Mainboard chỉ có %d khe.", ramQty, mbSlots)));
                failures.add("Số thanh RAM vượt quá số khe cắm của bo mạch chủ");
            }
        }

        // 5. Kiểm tra công suất nguồn (System Rule 3 - PSU Headroom)
        if (psu != null && cpu != null) {
            int cpuPower = Integer.parseInt(cpu.getSpecOrDefault("TDP_Power", "65"));
            int gpuPower = gpu != null ? Integer.parseInt(gpu.getSpecOrDefault("TDP_Power", "115")) : 0;
            int estLoad = cpuPower + gpuPower + 80;
            int recWatt = (int) Math.round(estLoad * 1.30);
            int psuWatt = Integer.parseInt(psu.getSpecOrDefault("Wattage", "500"));

            boolean psuOk = psuWatt >= recWatt;
            checks.add(new CompatibilityCheckResponse("Công suất Nguồn (PSU)", 
                psuOk ? "pass" : "fail", 
                psuOk ? "Nguồn " + psuWatt + "W (Tải ước tính " + estLoad + "W, an toàn >= " + recWatt + "W)"
                      : "NGUỒN YẾU! Công suất " + psuWatt + "W thấp hơn mức an toàn " + recWatt + "W"));
            if (!psuOk) failures.add("Nguồn không đủ công suất cung cấp cho hệ thống");
        }

        // 6. Kiểm tra ngân sách (System Rule 4 - Budget + 3% dung sai)
        BigDecimal total = parts.values().stream()
            .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxBudget = budget.multiply(BigDecimal.valueOf(1.03));
        boolean budgetOk = total.compareTo(maxBudget) <= 0;
        checks.add(new CompatibilityCheckResponse("Ngân sách", 
            budgetOk ? "pass" : "fail", 
            budgetOk ? "Tổng tiền: " + total + " ₫ / " + budget + " ₫"
                     : "VƯỢT NGÂN SÁCH! " + total + " ₫ vượt mức cho phép " + budget + " ₫"));
        if (!budgetOk) failures.add("Tổng giá trị linh kiện vượt quá ngân sách");

        boolean overall = failures.isEmpty();
        return new ValidationReport(overall, checks, failures);
    }

    private ComponentCandidateDTO findPartByFallbackCategory(Map<String, ComponentCandidateDTO> parts, Integer categoryId) {
        if (categoryId == null) return null;
        return switch (categoryId) {
            case 1 -> parts.get("cpu");
            case 2 -> parts.get("mainboard");
            case 3 -> parts.get("ram");
            case 4 -> parts.get("gpu");
            case 5 -> parts.get("ssd");
            case 6 -> parts.get("psu");
            case 7 -> parts.get("case");
            case 8 -> parts.get("cooler");
            default -> null;
        };
    }

    private String getSpecValue(ComponentCandidateDTO part, Integer specId) {
        if (part == null || specId == null) return null;
        String val = part.getSpecById(specId);
        if (val != null) return val;

        // Fallback tra cứu theo tên thuộc tính chuẩn nếu DTO chỉ chứa map specs dạng tên chuỗi
        String specName = switch (specId) {
            case 1 -> "Socket";
            case 2 -> "TDP_Power";
            case 4 -> "Memory_Type";
            case 5 -> "RAM_Slots";
            case 7 -> "Form_Factor";
            case 11 -> "Length_MM";
            case 14 -> "Wattage";
            case 16 -> "Max_GPU_Length_MM";
            case 17 -> "Max_Cooler_Height_MM";
            case 18 -> "Supported_Motherboards";
            case 19 -> "Height_MM";
            case 21 -> "Supported_Sockets";
            default -> null;
        };
        return specName != null ? part.getSpec(specName) : null;
    }

    private boolean evaluateOperator(String val1, String val2, String operator) {
        if (val1 == null || val2 == null) return false;
        val1 = val1.trim();
        val2 = val2.trim();

        return switch (operator.toUpperCase()) {
            case "EQUALS" -> val1.equalsIgnoreCase(val2);
            case "INCLUDES" -> val2.toLowerCase().contains(val1.toLowerCase()) || val1.toLowerCase().contains(val2.toLowerCase());
            case "LESS_THAN_OR_EQUAL" -> {
                try {
                    yield Double.parseDouble(val1) <= Double.parseDouble(val2);
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case "GREATER_THAN_OR_EQUAL" -> {
                try {
                    yield Double.parseDouble(val1) >= Double.parseDouble(val2);
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }
}
