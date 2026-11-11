package com.project.cybershop.order.api.dto.request;

import java.util.List;

// Thông tin nhập tại bước 5; không ghi DB ở bước này.
public record CheckoutDetailsRequest(
        String pickupName,
        String pickupPhone,
        String note,
        String promotionCode,
        List<Integer> cartItemIds,
        List<Integer> itemVariantIds
) {
    public CheckoutDetailsRequest(String pickupName, String pickupPhone, String note, String promotionCode, List<Integer> itemVariantIds) {
        this(pickupName, pickupPhone, note, promotionCode, null, itemVariantIds);
    }
    public CheckoutDetailsRequest(String pickupName, String pickupPhone, String note, String promotionCode) {
        this(pickupName, pickupPhone, note, promotionCode, null, null);
    }
}
