package com.project.cybershop.pcbuild.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CompatibleReplacementResponse {
    private Integer variantId;
    private String productName;
    private String variantName;
    private BigDecimal price;
    private String imageUrl;
    private Integer stockQuantity;
    private Map<String, String> specs;

    @JsonProperty("isCompatible")
    private boolean isCompatible;

    private String compatibilityNote;
}

