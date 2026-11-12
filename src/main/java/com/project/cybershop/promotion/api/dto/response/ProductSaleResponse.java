package com.project.cybershop.promotion.api.dto.response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record ProductSaleResponse(Integer productId, String productName, boolean configured, boolean enabled,
        BigDecimal percentage, LocalDateTime start, LocalDateTime end, boolean active, List<Variant> variants,
        String discountType, BigDecimal discountValue, List<Integer> variantIds) {
    public record Variant(Integer id, String name, BigDecimal basePrice, BigDecimal regularPrice, BigDecimal effectivePrice) {}
}
