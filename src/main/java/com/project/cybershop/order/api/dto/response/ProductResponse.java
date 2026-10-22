package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Integer variantId,
        Integer productId,
        String productName,
        String variantName,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        Integer warrantyPeriodMonths
) {
}

