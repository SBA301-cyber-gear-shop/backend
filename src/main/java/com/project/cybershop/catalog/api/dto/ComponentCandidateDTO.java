package com.project.cybershop.catalog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentCandidateDTO {
    private Integer variantId;
    private Integer productId;
    private Integer categoryId;   // ID danh mục linh kiện (1: cpu, 2: mainboard, 3: ram, 4: gpu, 5: ssd, 6: psu, 7: case, 8: cooler)
    private String categorySlug; // cpu, mainboard, ram, gpu, ssd, psu, case, cooler
    private String productName;
    private String variantName;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    
    @Builder.Default
    private int quantity = 1;

    @Builder.Default
    private Map<String, String> specs = new HashMap<>(); // Tên thông số -> Giá trị (VD: "Socket" -> "LGA1700")

    @Builder.Default
    private Map<Integer, String> specValuesById = new HashMap<>(); // ID thông số -> Giá trị (VD: 1 -> "LGA1700")

    public String getSpec(String key) {
        return specs.get(key);
    }

    public String getSpecOrDefault(String key, String defaultValue) {
        return specs.getOrDefault(key, defaultValue);
    }

    public String getSpecById(Integer specId) {
        return specValuesById.get(specId);
    }

    public String getSpecByIdOrDefault(Integer specId, String defaultValue) {
        return specValuesById.getOrDefault(specId, defaultValue);
    }
}
