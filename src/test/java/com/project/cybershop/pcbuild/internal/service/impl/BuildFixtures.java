package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.PurposeMeta;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu dựng tay cho test bước 4–6: khuôn 11 slot và 12 luật chép đúng seed (mục 9, 13),
 * kho mẫu theo ví dụ 20.000.000đ GAMING của docx bước 5 (Màn-A, CPU-A, Main-A… cùng giá).
 */
final class BuildFixtures {

    /** category_id theo step_order 1–11: màn, phím, chuột, CPU, main, RAM, SSD, GPU, tản, case, PSU. */
    static final int[] CATEGORY_BY_STEP = {9, 10, 11, 1, 2, 3, 5, 4, 8, 7, 6};
    static final String[] CODE_BY_STEP = {"MONITOR", "KEYBOARD", "MOUSE", "CPU", "MAINBOARD", "RAM", "SSD", "GPU",
            "COOLER", "CASE", "PSU"};
    static final int[] MAX_QTY_BY_STEP = {1, 1, 1, 2, 1, 8, 6, 4, 2, 1, 1};
    static final int[] GAMING_SHARES = {12, 4, 3, 15, 9, 8, 6, 28, 3, 5, 7};
    static final int[] OFFICE_SHARES = {16, 4, 3, 23, 14, 12, 12, 0, 4, 5, 7};

    // Id biến thể trong kho mẫu
    static final int MONITOR_A = 201;
    static final int MONITOR_CHEAP = 202;
    static final int MONITOR_EXPENSIVE = 203;
    static final int KEYBOARD_A = 301;
    static final int KEYBOARD_CHEAP = 302;
    static final int MOUSE_A = 401;
    static final int MOUSE_CHEAP = 402;
    static final int CPU_A = 12;
    static final int CPU_EXPENSIVE = 13;
    static final int CPU_INTEL_IGPU = 14;
    static final int MAIN_A = 31;
    static final int MAIN_INTEL = 32;
    static final int RAM_A = 45;
    static final int RAM_DDR4 = 46;
    static final int SSD_A = 51;
    static final int SSD_CHEAP = 52;
    static final int GPU_A = 61;
    static final int GPU_CHEAP = 62;
    static final int COOLER_A = 81;
    static final int COOLER_CHEAP = 82;
    static final int CASE_A = 71;
    static final int CASE_CHEAP = 72;
    static final int PSU_650 = 91;
    static final int PSU_550 = 92;
    static final int PSU_1000 = 93;

    private BuildFixtures() {
    }

    static List<SlotMeta> slots() {
        List<SlotMeta> slots = new ArrayList<>();
        for (int step = 0; step < CATEGORY_BY_STEP.length; step++) {
            int categoryId = CATEGORY_BY_STEP[step];
            boolean gpu = categoryId == 4;
            slots.add(SlotMeta.builder()
                    .slotId(categoryId)
                    .categoryId(categoryId)
                    .componentCode(CODE_BY_STEP[step])
                    .label(CODE_BY_STEP[step])
                    .stepOrder(step + 1)
                    .required(!gpu)
                    .minQty(gpu ? 0 : 1)
                    .maxQty(MAX_QTY_BY_STEP[step])
                    .requiredIfCategoryId(gpu ? 1 : null)
                    .requiredIfSpecId(gpu ? 26 : null)
                    .requiredIfValue(gpu ? "NO" : null)
                    .build());
        }
        return slots;
    }

    static BuildMetadata metadata() {
        Map<String, PurposeMeta> purposes = new LinkedHashMap<>();
        purposes.put("GAMING", purpose("GAMING", 16_000_000L, GAMING_SHARES));
        purposes.put("OFFICE", purpose("OFFICE", 12_500_000L, OFFICE_SHARES));
        return BuildMetadata.builder().slots(slots()).purposes(purposes).maxBuildPrice(100_000_000L).build();
    }

    static PurposeMeta purpose(String code, long minBudget, int[] sharesByStep) {
        Map<Integer, BigDecimal> shares = new HashMap<>();
        for (int step = 0; step < CATEGORY_BY_STEP.length; step++) {
            shares.put(CATEGORY_BY_STEP[step], BigDecimal.valueOf(sharesByStep[step]).setScale(2));
        }
        return PurposeMeta.builder().purpose(code).label(code).minBudget(minBudget).sharePctBySlotId(shares).build();
    }

    /** 12 luật của seed mục 9. */
    static List<CompatibilityRule> rules() {
        return List.of(
                new CompatibilityRule(1, "CPU ↔ Bo mạch chủ (Socket)", 1, 1, 2, 1, "EQUALS"),
                new CompatibilityRule(2, "RAM ↔ Bo mạch chủ (Loại RAM)", 3, 4, 2, 4, "EQUALS"),
                new CompatibilityRule(3, "Chiều dài VGA trong Case", 4, 11, 7, 16, "LESS_THAN_OR_EQUAL"),
                new CompatibilityRule(4, "Chiều cao Tản khí trong Case", 8, 19, 7, 17, "LESS_THAN_OR_EQUAL"),
                new CompatibilityRule(5, "CPU ↔ Tản nhiệt (Socket hỗ trợ)", 1, 1, 8, 21, "INCLUDES"),
                new CompatibilityRule(6, "Bo mạch chủ vừa Case (Form factor)", 2, 7, 7, 18, "INCLUDES"),
                new CompatibilityRule(7, "Số thanh RAM trong kit ≤ số khe", 3, 34, 2, 5, "LESS_THAN_OR_EQUAL"),
                new CompatibilityRule(8, "Dung lượng kit RAM ≤ RAM tối đa của bo", 3, 9, 2, 6, "LESS_THAN_OR_EQUAL"),
                new CompatibilityRule(9, "Nguồn ≥ công suất khuyến nghị của VGA", 4, 50, 6, 14, "LESS_THAN_OR_EQUAL"),
                new CompatibilityRule(10, "Radiator tản nước vừa Case", 7, 59, 8, 60, "GREATER_THAN_OR_EQUAL"),
                new CompatibilityRule(11, "SSD M.2 cần khe M.2 trên bo", 5, 40, 2, 8, "LESS_THAN_OR_EQUAL"),
                new CompatibilityRule(12, "CPU hỗ trợ đủ số socket của bo", 1, 28, 2, 31, "GREATER_THAN_OR_EQUAL"));
    }

    /** Kho mẫu: món của ví dụ docx + món rẻ hơn (để có giá rẻ nhất, chừa tiền) + món đắt hơn trần. */
    static List<ComponentCandidateDTO> catalog() {
        List<ComponentCandidateDTO> parts = new ArrayList<>();
        parts.add(part(MONITOR_A, 9, "Màn-A", 2_350_000, 10, Map.of()));
        parts.add(part(MONITOR_CHEAP, 9, "Màn rẻ", 1_490_000, 10, Map.of()));
        parts.add(part(MONITOR_EXPENSIVE, 9, "Màn đắt", 3_000_000, 10, Map.of()));
        parts.add(part(KEYBOARD_A, 10, "Phím-A", 800_000, 10, Map.of()));
        parts.add(part(KEYBOARD_CHEAP, 10, "Phím rẻ", 190_000, 10, Map.of()));
        parts.add(part(MOUSE_A, 11, "Chuột-A", 600_000, 10, Map.of()));
        parts.add(part(MOUSE_CHEAP, 11, "Chuột rẻ", 120_000, 10, Map.of()));
        parts.add(cpu(CPU_A, "CPU-A", 3_000_000, "AM5", "65", "NO"));
        parts.add(cpu(CPU_EXPENSIVE, "CPU đắt", 3_500_000, "AM5", "105", "NO"));
        parts.add(cpu(CPU_INTEL_IGPU, "CPU Intel iGPU", 2_000_000, "LGA1700", "65", "YES"));
        parts.add(mainboard(MAIN_A, "Main-A", 1_800_000, "AM5", "DDR5", "2", "MICRO_ATX"));
        parts.add(mainboard(MAIN_INTEL, "Main Intel", 1_500_000, "LGA1700", "DDR4", "4", "STD_ATX"));
        parts.add(ram(RAM_A, "RAM DDR5 8GB", 800_000, "DDR5", "8"));
        parts.add(ram(RAM_DDR4, "RAM DDR4 8GB", 500_000, "DDR4", "8"));
        parts.add(part(SSD_A, 5, "SSD 1TB", 1_200_000, 10, Map.of(40, "1", 9, "1000")));
        parts.add(part(SSD_CHEAP, 5, "SSD 512GB", 600_000, 10, Map.of(40, "1", 9, "512")));
        parts.add(gpu(GPU_A, "GPU-A", 5_600_000, "250", "160"));
        parts.add(gpu(GPU_CHEAP, "GPU rẻ", 3_000_000, "240", "120"));
        parts.add(airCooler(COOLER_A, "Tản-A", 650_000, "155"));
        parts.add(airCooler(COOLER_CHEAP, "Tản rẻ", 300_000, "150"));
        parts.add(pcCase(CASE_A, "Case-A", 950_000, "330", "160"));
        parts.add(pcCase(CASE_CHEAP, "Case rẻ", 700_000, "330", "160"));
        parts.add(psu(PSU_650, "PSU 650W", 1_350_000, "650"));
        parts.add(psu(PSU_550, "PSU 550W", 900_000, "550"));
        parts.add(psu(PSU_1000, "PSU 1000W", 2_500_000, "1000"));
        return parts;
    }

    static ComponentCandidateDTO part(int variantId, int categoryId, String name, long price, int stock,
                                      Map<Integer, String> specs) {
        return ComponentCandidateDTO.builder()
                .variantId(variantId)
                .categoryId(categoryId)
                .productName(name)
                .price(BigDecimal.valueOf(price).setScale(2))
                .stockQuantity(stock)
                .specValuesById(new HashMap<>(specs))
                .build();
    }

    static ComponentCandidateDTO cpu(int id, String name, long price, String socket, String tdp, String hasIgpu) {
        return part(id, 1, name, price, 10, Map.of(1, socket, 2, tdp, 26, hasIgpu, 28, "1"));
    }

    static ComponentCandidateDTO mainboard(int id, String name, long price, String socket, String memoryType,
                                           String ramSlots, String formFactor) {
        return part(id, 2, name, price, 10, Map.of(1, socket, 4, memoryType, 5, ramSlots, 6, "64", 7, formFactor,
                8, "1", 31, "1", 32, "1"));
    }

    static ComponentCandidateDTO ram(int id, String name, long price, String memoryType, String capacityGb) {
        return part(id, 3, name, price, 10, Map.of(4, memoryType, 9, capacityGb, 34, "1"));
    }

    static ComponentCandidateDTO gpu(int id, String name, long price, String lengthMm, String tdp) {
        return part(id, 4, name, price, 10, Map.of(11, lengthMm, 2, tdp, 50, "550", 54, "1"));
    }

    static ComponentCandidateDTO airCooler(int id, String name, long price, String heightMm) {
        return part(id, 8, name, price, 10, Map.of(20, "Air", 21, "AM4,AM5,LGA1700", 19, heightMm, 60, "0"));
    }

    static ComponentCandidateDTO pcCase(int id, String name, long price, String maxGpuMm, String maxCoolerMm) {
        return part(id, 7, name, price, 10, Map.of(18, "STD_ATX,MICRO_ATX", 16, maxGpuMm, 17, maxCoolerMm, 59, "240"));
    }

    static ComponentCandidateDTO psu(int id, String name, long price, String wattage) {
        return part(id, 6, name, price, 10, Map.of(14, wattage));
    }

    static ComponentCandidateDTO byId(List<ComponentCandidateDTO> parts, int variantId) {
        return parts.stream().filter(part -> part.getVariantId() == variantId).findFirst().orElseThrow();
    }

    static SlotMeta slot(int categoryId) {
        return slots().stream().filter(slot -> slot.getCategoryId() == categoryId).findFirst().orElseThrow();
    }

    static BuildLine line(ComponentCandidateDTO part, int quantity) {
        return new BuildLine(slot(part.getCategoryId()), part, quantity);
    }

    static BuildContext context(List<ComponentCandidateDTO> parts) {
        return new BuildContext(metadata(), parts, rules());
    }

    /** Bộ PC của ví dụ docx: tổng 19.900.000đ. */
    static List<BuildLine> docxBuild() {
        List<ComponentCandidateDTO> parts = catalog();
        List<BuildLine> lines = new ArrayList<>();
        lines.add(line(byId(parts, MONITOR_A), 1));
        lines.add(line(byId(parts, KEYBOARD_A), 1));
        lines.add(line(byId(parts, MOUSE_A), 1));
        lines.add(line(byId(parts, CPU_A), 1));
        lines.add(line(byId(parts, MAIN_A), 1));
        lines.add(line(byId(parts, RAM_A), 2));
        lines.add(line(byId(parts, SSD_A), 1));
        lines.add(line(byId(parts, GPU_A), 1));
        lines.add(line(byId(parts, COOLER_A), 1));
        lines.add(line(byId(parts, CASE_A), 1));
        lines.add(line(byId(parts, PSU_650), 1));
        return lines;
    }

    /** Thay một món trong danh sách bằng bản có thông số khác (giữ nguyên id, tên, giá). */
    static ComponentCandidateDTO withSpec(ComponentCandidateDTO part, int specId, String value) {
        Map<Integer, String> specs = new HashMap<>(part.getSpecValuesById());
        if (value == null) {
            specs.remove(specId);
        } else {
            specs.put(specId, value);
        }
        return part(part.getVariantId(), part.getCategoryId(), part.getProductName(),
                part.getPrice().longValueExact(), part.getStockQuantity(), specs);
    }
}
