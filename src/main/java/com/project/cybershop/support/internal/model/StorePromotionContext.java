package com.project.cybershop.support.internal.model;

import java.math.BigDecimal;
import java.util.List;

public record StorePromotionContext(
        List<PromotionSummary> activeVouchers,
        List<FlashSaleSummary> activeFlashSales
) {
    public record PromotionSummary(
            String code,
            String name,
            String discountType,
            BigDecimal discountValue,
            String endDate
    ) {}

    public record FlashSaleSummary(
            Integer productId,
            Integer variantId,
            String productName,
            String variantName,
            double livePrice,
            double originalPrice,
            String discountDesc,
            String imageUrl
    ) {}
}
