package com.project.cybershop.order.api.dto.request;

import java.util.List;

public record CheckoutValidateRequest(
        List<Integer> cartItemIds,
        List<Integer> itemVariantIds
) {
    public CheckoutValidateRequest(List<Integer> itemVariantIds) {
        this(null, itemVariantIds);
    }
}
