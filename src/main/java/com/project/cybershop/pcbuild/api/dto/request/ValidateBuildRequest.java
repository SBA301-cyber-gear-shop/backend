package com.project.cybershop.pcbuild.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ValidateBuildRequest {
    private BigDecimal budget;
    private List<ItemPart> items;

    @Data
    public static class ItemPart {
        private String role;
        private Integer variantId;
        private Integer quantity;
    }
}
