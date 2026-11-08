package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer id, String orderCode, Integer userId, String status,
        String pickupName, String pickupPhone, String note, String cancelReason,
        BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
        String promotionCode, String paymentStatus, String methodType,
        LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime paidAt,
        List<Item> items,
        String paymentUrl,
        String deliveryType,
        BigDecimal shippingFee,
        String shippingAddress,
        String trackingCode,
        String carrier
) {
    public OrderResponse(
            Integer id, String orderCode, Integer userId, String status,
            String pickupName, String pickupPhone, String note,
            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
            String promotionCode, String paymentStatus, String methodType,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime paidAt,
            List<Item> items
    ) {
        this(id, orderCode, userId, status, pickupName, pickupPhone, note, null,
             totalAmount, discountAmount, finalAmount, promotionCode, paymentStatus, methodType,
             createdAt, updatedAt, paidAt, items, null, "store", BigDecimal.ZERO, null, null, "GHN");
    }

    public OrderResponse(
            Integer id, String orderCode, Integer userId, String status,
            String pickupName, String pickupPhone, String note,
            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
            String promotionCode, String paymentStatus, String methodType,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime paidAt,
            List<Item> items,
            String paymentUrl
    ) {
        this(id, orderCode, userId, status, pickupName, pickupPhone, note, null,
             totalAmount, discountAmount, finalAmount, promotionCode, paymentStatus, methodType,
             createdAt, updatedAt, paidAt, items, paymentUrl, "store", BigDecimal.ZERO, null, null, "GHN");
    }

    public record Item(Integer id, Integer productVariantId, String productName, int quantity,
                       BigDecimal unitPrice, BigDecimal subtotal, int warrantyPeriodMonths,
                       Integer rating, String reviewComment) {}
}
