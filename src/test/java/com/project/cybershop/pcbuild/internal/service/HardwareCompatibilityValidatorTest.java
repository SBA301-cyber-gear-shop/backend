package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HardwareCompatibilityValidator - Kiểm thử 100% quy tắc tương thích phần cứng (Dynamic + System)")
class HardwareCompatibilityValidatorTest {

    @Mock
    private CompatibilityRuleRepository ruleRepository;

    private HardwareCompatibilityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HardwareCompatibilityValidator(ruleRepository);

        // Nạp 4 quy tắc động mẫu từ bảng compatibility_rules
        List<CompatibilityRule> defaultRules = List.of(
            new CompatibilityRule(1, "CPU ↔ Bo mạch chủ (Socket)", 1, 1, 2, 1, "EQUALS"),
            new CompatibilityRule(2, "RAM ↔ Bo mạch chủ (Loại RAM)", 3, 4, 2, 4, "EQUALS"),
            new CompatibilityRule(3, "Chiều dài VGA trong Case", 4, 11, 7, 16, "LESS_THAN_OR_EQUAL"),
            new CompatibilityRule(4, "Chiều cao Tản nhiệt trong Case", 8, 19, 7, 17, "LESS_THAN_OR_EQUAL")
        );
        when(ruleRepository.findAll()).thenReturn(defaultRules);
    }

    private ComponentCandidateDTO createPart(Integer catId, String slug, String name, BigDecimal price, int qty, 
                                             Map<String, String> specs, Map<Integer, String> specIds) {
        return ComponentCandidateDTO.builder()
                .variantId((int) (Math.random() * 1000) + 1)
                .categoryId(catId)
                .categorySlug(slug)
                .productName(name)
                .variantName("Default Variant")
                .price(price)
                .quantity(qty)
                .specs(specs != null ? new HashMap<>(specs) : new HashMap<>())
                .specValuesById(specIds != null ? new HashMap<>(specIds) : new HashMap<>())
                .build();
    }

    private Map<String, ComponentCandidateDTO> createStandardParts() {
        Map<String, ComponentCandidateDTO> parts = new HashMap<>();
        parts.put("cpu", createPart(1, "cpu", "Intel i5-13400F", new BigDecimal("4690000"), 1, 
                Map.of("Socket", "LGA1700", "TDP_Power", "148"),
                Map.of(1, "LGA1700", 2, "148")));
        parts.put("mainboard", createPart(2, "mainboard", "MSI PRO B760M DDR5", new BigDecimal("3490000"), 1, 
                Map.of("Socket", "LGA1700", "Memory_Type", "DDR5", "RAM_Slots", "4"),
                Map.of(1, "LGA1700", 4, "DDR5", 5, "4")));
        parts.put("ram", createPart(3, "ram", "Kingston 16GB DDR5", new BigDecimal("1290000"), 2, 
                Map.of("Memory_Type", "DDR5"),
                Map.of(4, "DDR5")));
        parts.put("gpu", createPart(4, "gpu", "RTX 4060 8GB", new BigDecimal("7990000"), 1, 
                Map.of("TDP_Power", "115", "Length_MM", "192"),
                Map.of(2, "115", 11, "192")));
        parts.put("ssd", createPart(5, "ssd", "SSD NV2 1TB", new BigDecimal("1690000"), 1, 
                Map.of("Capacity_GB", "1000"),
                Map.of(9, "1000")));
        parts.put("psu", createPart(6, "psu", "MSI 650W", new BigDecimal("1390000"), 1, 
                Map.of("Wattage", "650"),
                Map.of(14, "650")));
        parts.put("case", createPart(7, "case", "Montech AIR 100", new BigDecimal("1250000"), 1, 
                Map.of("Max_GPU_Length_MM", "330", "Max_Cooler_Height_MM", "161"),
                Map.of(16, "330", 17, "161")));
        parts.put("cooler", createPart(8, "cooler", "DeepCool AG400", new BigDecimal("550000"), 1, 
                Map.of("Cooler_Type", "Air", "Height_MM", "150"),
                Map.of(19, "150", 20, "Air")));
        return parts;
    }

    @Test
    @DisplayName("Case 1: Cấu hình chuẩn đầy đủ linh kiện, tương thích 100% -> Pass toàn bộ")
    void shouldPassWhenAllComponentsAreCompatible() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        BigDecimal budget = new BigDecimal("25000000");

        var report = validator.validate(parts, budget);

        assertThat(report.isValid()).isTrue();
        assertThat(report.failureReasons()).isEmpty();
        assertThat(report.checks()).allMatch(c -> "pass".equals(c.getStatus()));
    }

    @Test
    @DisplayName("Case 2: Thiếu linh kiện cốt lõi (ví dụ thiếu Case hoặc PSU) -> Báo lỗi Rule 1")
    void shouldFailWhenMissingCoreParts() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.remove("case");

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).contains("Thiếu linh kiện bắt buộc");
    }

    @Test
    @DisplayName("Case 3: CPU và Bo mạch chủ lệch Socket -> Báo lỗi quy tắc động Socket")
    void shouldFailWhenSocketMismatch() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("cpu", createPart(1, "cpu", "Ryzen 5 7600", new BigDecimal("5000000"), 1, 
                Map.of("Socket", "AM5", "TDP_Power", "65"),
                Map.of(1, "AM5", 2, "65")));

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).anyMatch(f -> f.contains("Socket"));
    }

    @Test
    @DisplayName("Case 4: Chuẩn RAM lệch so với Mainboard (DDR4 cắm vào DDR5) -> Báo lỗi quy tắc động RAM")
    void shouldFailWhenRamTypeMismatch() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("ram", createPart(3, "ram", "Corsair DDR4 16GB", new BigDecimal("1000000"), 2, 
                Map.of("Memory_Type", "DDR4"),
                Map.of(4, "DDR4")));

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).anyMatch(f -> f.contains("Loại RAM"));
    }

    @Test
    @DisplayName("Case 5: Số lượng thanh RAM vượt quá số khe cắm của Bo mạch chủ -> Báo lỗi System Rule RAM Slots")
    void shouldFailWhenRamQuantityExceedsSlots() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("ram", createPart(3, "ram", "Kingston DDR5", new BigDecimal("1290000"), 6, 
                Map.of("Memory_Type", "DDR5"),
                Map.of(4, "DDR5"))); // 6 thanh trong khi Mainboard chỉ có 4 khe

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).contains("Số thanh RAM vượt quá số khe cắm của bo mạch chủ");
    }

    @Test
    @DisplayName("Case 6: Công suất nguồn không đủ so với tải hệ thống -> Báo lỗi System Rule PSU")
    void shouldFailWhenPsuWattageIsInsufficient() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("psu", createPart(6, "psu", "Nguồn cỏ 300W", new BigDecimal("400000"), 1, 
                Map.of("Wattage", "300"),
                Map.of(14, "300")));

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).contains("Nguồn không đủ công suất cung cấp cho hệ thống");
    }

    @Test
    @DisplayName("Case 7: Hệ thống không có GPU rời (chạy iGPU) -> Tính tải nguồn an toàn theo CPU")
    void shouldCalculatePsuWithoutGpuCorrectly() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.remove("gpu");

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.checks()).anyMatch(c -> c.getLabel().contains("PSU") && "pass".equals(c.getStatus()));
    }

    @Test
    @DisplayName("Case 8: Chiều dài card VGA vượt quá giới hạn vỏ Case -> Báo lỗi quy tắc động VGA")
    void shouldFailWhenGpuIsTooLongForCase() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("gpu", createPart(4, "gpu", "RTX 4090 Dài", new BigDecimal("40000000"), 1, 
                Map.of("TDP_Power", "450", "Length_MM", "360"),
                Map.of(2, "450", 11, "360"))); // Dài 360mm vs Case 330mm

        var report = validator.validate(parts, new BigDecimal("50000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).anyMatch(f -> f.contains("VGA"));
    }

    @Test
    @DisplayName("Case 9: Tản nhiệt khí vượt quá chiều cao tối đa của Case -> Báo lỗi quy tắc động Tản nhiệt")
    void shouldFailWhenAirCoolerIsTooTallForCase() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("cooler", createPart(8, "cooler", "Tản khí khổng lồ", new BigDecimal("1200000"), 1, 
                Map.of("Cooler_Type", "Air", "Height_MM", "175"),
                Map.of(19, "175", 20, "Air"))); // 175mm vs Case 161mm

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).anyMatch(f -> f.contains("Tản nhiệt"));
    }

    @Test
    @DisplayName("Case 10: Tản nhiệt nước AIO -> Bỏ qua kiểm tra chiều cao tản khí")
    void shouldIgnoreCoolerHeightForAioLiquidCooler() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        parts.put("cooler", createPart(8, "cooler", "Tản nhiệt nước 240mm", new BigDecimal("1500000"), 1, 
                Map.of("Cooler_Type", "Liquid", "Height_MM", "277"),
                Map.of(19, "277", 20, "Liquid")));

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.checks()).noneMatch(c -> "fail".equals(c.getStatus()) && c.getLabel().contains("Tản nhiệt"));
    }

    @Test
    @DisplayName("Case 11: Tổng tiền vượt quá ngân sách cộng dung sai 3% -> Báo lỗi Rule Ngân sách")
    void shouldFailWhenTotalExceedsBudgetWithTolerance() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        BigDecimal tightBudget = new BigDecimal("20000000");

        var report = validator.validate(parts, tightBudget);

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).contains("Tổng giá trị linh kiện vượt quá ngân sách");
    }

    @Test
    @DisplayName("Case 12: Tổng tiền nằm trong dung sai 3% (ví dụ 101% ngân sách) -> Pass Rule Ngân sách")
    void shouldPassWhenTotalIsWithinBudgetTolerance() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        BigDecimal nearBudget = new BigDecimal("23000000");

        var report = validator.validate(parts, nearBudget);

        assertThat(report.checks()).anyMatch(c -> c.getLabel().contains("Ngân sách") && "pass".equals(c.getStatus()));
    }

    @Test
    @DisplayName("Case 13: Map linh kiện rỗng hoàn toàn -> Fail toàn bộ và trả về báo cáo chuẩn")
    void shouldHandleEmptyPartsMap() {
        var report = validator.validate(new HashMap<>(), new BigDecimal("10000000"));

        assertThat(report.isValid()).isFalse();
        assertThat(report.failureReasons()).contains("Thiếu linh kiện bắt buộc");
    }

    @Test
    @DisplayName("Case 14: Thẩm định quy tắc động với toán tử INCLUDES (Ví dụ chuẩn mainboard hỗ trợ)")
    void shouldPassDynamicRuleWithIncludesOperator() {
        Map<String, ComponentCandidateDTO> parts = createStandardParts();
        List<CompatibilityRule> customRules = List.of(
            new CompatibilityRule(99, "Form Factor Hỗ trợ", 2, 7, 7, 18, "INCLUDES")
        );
        when(ruleRepository.findAll()).thenReturn(customRules);

        parts.get("mainboard").getSpecs().put("Form_Factor", "mATX");
        parts.get("case").getSpecs().put("Supported_Motherboards", "mATX, ITX");

        var report = validator.validate(parts, new BigDecimal("25000000"));

        assertThat(report.checks()).anyMatch(c -> "Form Factor Hỗ trợ".equals(c.getLabel()) && "pass".equals(c.getStatus()));
    }
}
