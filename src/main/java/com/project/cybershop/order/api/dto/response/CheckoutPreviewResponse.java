package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        Integer cartId,
        List<CheckoutItemResponse> items,
        BigDecimal totalAmount,
        boolean valid
) {}
