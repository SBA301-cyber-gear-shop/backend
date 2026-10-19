package com.project.cybershop.order.api.dto;

// Thông tin nhập tại bước 5; không ghi DB ở bước này.
public record CheckoutDetailsRequest(
        String pickupName,
        String pickupPhone,
        String note,
        String promotionCode
) {}
