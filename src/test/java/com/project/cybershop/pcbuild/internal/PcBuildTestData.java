package com.project.cybershop.pcbuild.internal;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.RuleDefinition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dữ liệu dựng tay cho test engine: luật R1–R12 (DD 4.5) và thông số bắt buộc (DD 4.3), không cần DB. */
public final class PcBuildTestData {

    public static final List<RuleDefinition> RULES = List.of(
            new RuleDefinition(1, "CPU ↔ Bo mạch chủ (Socket)", 1, 1, 2, 1, "EQUALS"),
            new RuleDefinition(2, "RAM ↔ Bo mạch chủ (Loại RAM)", 3, 4, 2, 4, "EQUALS"),
            new RuleDefinition(3, "Chiều dài VGA trong Case", 4, 11, 7, 16, "LESS_THAN_OR_EQUAL"),
            new RuleDefinition(4, "Chiều cao Tản khí trong Case", 8, 19, 7, 17, "LESS_THAN_OR_EQUAL"),
            new RuleDefinition(5, "CPU ↔ Tản nhiệt (Socket hỗ trợ)", 1, 1, 8, 21, "INCLUDES"),
            new RuleDefinition(6, "Bo mạch chủ vừa Case (Form factor)", 2, 7, 7, 18, "INCLUDES"),
            new RuleDefinition(7, "Số thanh RAM trong kit ≤ số khe", 3, 34, 2, 5, "LESS_THAN_OR_EQUAL"),
            new RuleDefinition(8, "Dung lượng kit RAM ≤ RAM tối đa của bo", 3, 9, 2, 6, "LESS_THAN_OR_EQUAL"),
            new RuleDefinition(9, "Nguồn ≥ công suất khuyến nghị của VGA", 4, 50, 6, 14, "LESS_THAN_OR_EQUAL"),
            new RuleDefinition(10, "Radiator tản nước vừa Case", 7, 59, 8, 60, "GREATER_THAN_OR_EQUAL"),
            new RuleDefinition(11, "SSD M.2 cần khe M.2 trên bo", 5, 40, 2, 8, "LESS_THAN_OR_EQUAL"),
            new RuleDefinition(12, "CPU hỗ trợ đủ số socket của bo", 1, 28, 2, 31, "GREATER_THAN_OR_EQUAL"));

    public static final Map<Integer, Set<Integer>> REQUIRED_SPECS = Map.of(
            1, Set.of(1, 2, 28),
            2, Set.of(1, 4, 5, 6, 7, 8, 31),
            3, Set.of(4, 9, 34),
            4, Set.of(2, 11, 50),
            5, Set.of(40),
            6, Set.of(14),
            7, Set.of(16, 17, 18, 59),
            8, Set.of(19, 20, 21, 60));

    private static final Map<String, Integer> CATEGORY_IDS = Map.of(
            "cpu", 1, "mainboard", 2, "ram", 3, "gpu", 4, "ssd", 5, "psu", 6, "case", 7, "cooler", 8);

    private PcBuildTestData() {
    }

    public static ComponentCandidateDTO part(int variantId, String slug, String name, long price, Map<Integer, String> specsById) {
        return ComponentCandidateDTO.builder()
                .variantId(variantId)
                .categoryId(CATEGORY_IDS.get(slug))
                .categorySlug(slug)
                .productName(name)
                .variantName("Tiêu chuẩn")
                .price(BigDecimal.valueOf(price))
                .stockQuantity(25)
                .specs(new HashMap<>())
                .specValuesById(new HashMap<>(specsById))
                .build();
    }

    /** Bộ desktop AM5 hợp lệ: qua mọi luật R1–R12 và J; nguồn cần ceil((120 + 200 + 80) × 1,3) = 520 W, card khuyến nghị 650 W. */
    public static Map<String, ComponentCandidateDTO> validDesktop() {
        Map<String, ComponentCandidateDTO> parts = new HashMap<>();
        parts.put("cpu", part(1, "cpu", "Ryzen 5 7600", 5_000_000,
                Map.of(1, "AM5", 2, "120", 26, "YES", 27, "NO", 28, "1", 66, "LISTED")));
        parts.put("mainboard", part(2, "mainboard", "B650M", 4_000_000,
                Map.of(1, "AM5", 4, "DDR5", 5, "4", 6, "192", 7, "MICRO_ATX", 8, "2", 30, "B650", 31, "1", 32, "1")));
        parts.put("ram", part(3, "ram", "DDR5 32GB (2x16)", 3_000_000, Map.of(4, "DDR5", 9, "32", 34, "2")));
        parts.put("gpu", part(4, "gpu", "RTX 5060", 9_000_000,
                Map.of(2, "200", 11, "300", 50, "650", 53, "OPEN_AIR", 54, "1")));
        parts.put("ssd", part(5, "ssd", "NVMe 1TB", 2_000_000, Map.of(40, "1")));
        parts.put("psu", part(6, "psu", "PSU 750W", 2_000_000, Map.of(14, "750")));
        parts.put("case", part(7, "case", "Case mATX", 1_500_000,
                Map.of(16, "380", 17, "170", 18, "STD_ATX,MICRO_ATX,MINI_ITX", 59, "360")));
        parts.put("cooler", part(8, "cooler", "Tản khí 155", 500_000,
                Map.of(19, "155", 20, "Air", 21, "AM4,AM5,LGA1700", 60, "0")));
        return parts;
    }

    public static List<BuildLine> lines(Map<String, ComponentCandidateDTO> parts) {
        List<BuildLine> lines = new ArrayList<>();
        for (String role : List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler")) {
            ComponentCandidateDTO part = parts.get(role);
            if (part != null) {
                lines.add(new BuildLine(role, part, 1));
            }
        }
        return lines;
    }

    public static CompatibilitySnapshot snapshot(List<RuleDefinition> rules, List<ComponentCandidateDTO> candidates) {
        Map<Integer, ParsedSpecs> parsed = new HashMap<>();
        candidates.forEach(candidate -> parsed.put(candidate.getVariantId(), ParsedSpecs.of(candidate)));
        return new CompatibilitySnapshot(candidates, parsed, rules, REQUIRED_SPECS,
                Map.of(1, Map.of("AM5", "AM5"), 53, Map.of("OPEN_AIR", "Tản mở", "BLOWER", "Blower")),
                Map.of(1, "Socket", 2, "TDP_Power", 16, "Max_GPU_Length_MM", 19, "Height_MM"),
                null, null, Instant.parse("2026-10-30T00:00:00Z"));
    }

    public static CompatibilitySnapshot snapshot() {
        return snapshot(RULES, List.of());
    }

    /** Bản sao part với một thông số đổi giá trị (null = xóa thông số). */
    public static ComponentCandidateDTO withSpec(ComponentCandidateDTO part, int specId, String value) {
        Map<Integer, String> specs = new HashMap<>(part.getSpecValuesById());
        if (value == null) {
            specs.remove(specId);
        } else {
            specs.put(specId, value);
        }
        return ComponentCandidateDTO.builder()
                .variantId(part.getVariantId()).categoryId(part.getCategoryId()).categorySlug(part.getCategorySlug())
                .productName(part.getProductName()).variantName(part.getVariantName()).price(part.getPrice())
                .stockQuantity(part.getStockQuantity()).quantity(part.getQuantity())
                .specs(new HashMap<>(part.getSpecs())).specValuesById(specs)
                .build();
    }
}
