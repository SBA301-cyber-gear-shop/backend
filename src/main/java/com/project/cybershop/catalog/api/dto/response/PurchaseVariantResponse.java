package com.project.cybershop.catalog.api.dto.response;

import java.math.BigDecimal;

public record PurchaseVariantResponse(
        Integer variantId,
        String productName,
        String variantName,
        BigDecimal price,
        Integer warrantyPeriodMonths,
        String status,
        Integer stockQuantity
) {
}
