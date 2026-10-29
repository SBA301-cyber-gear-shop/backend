package com.project.cybershop.order.api.dto.request;

import java.util.List;

public record CheckoutValidateRequest(
        List<Integer> itemVariantIds
) {}
