package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedPartResponse {
    private String role; // cpu, mainboard, ram, gpu, ssd, psu, case, cooler
    private Integer variantId;
    private String productName;
    private String variantName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private Map<String, String> specs;
    private String aiReason;
}
