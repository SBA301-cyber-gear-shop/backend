package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer id, String orderCode, Integer userId, String status,
        String pickupName, String pickupPhone, String note,
        BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
        String promotionCode, String paymentStatus, String methodType,
        LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime paidAt,
        List<Item> items,
        String paymentUrl
) {
    public OrderResponse(
            Integer id, String orderCode, Integer userId, String status,
            String pickupName, String pickupPhone, String note,
            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount,
            String promotionCode, String paymentStatus, String methodType,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime paidAt,
            List<Item> items
    ) {
        this(id, orderCode, userId, status, pickupName, pickupPhone, note,
             totalAmount, discountAmount, finalAmount, promotionCode, paymentStatus, methodType,
             createdAt, updatedAt, paidAt, items, null);
    }

    public record Item(Integer id, Integer productVariantId, String productName, int quantity,
                       BigDecimal unitPrice, BigDecimal subtotal, int warrantyPeriodMonths,
                       Integer rating, String reviewComment) {}
}
