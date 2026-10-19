package com.project.cybershop.order.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutDetailsResponse(
        Integer cartId,
        String pickupName,
        String pickupPhone,
        String note,
        String promotionCode,
        List<CheckoutItemResponse> items,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {}
