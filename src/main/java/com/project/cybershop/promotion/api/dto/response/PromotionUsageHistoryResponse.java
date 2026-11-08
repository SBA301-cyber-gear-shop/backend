package com.project.cybershop.promotion.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionUsageHistoryResponse(Integer promotionId, String code, int page, int size,
        long totalElements, int totalPages, List<Item> items) {
    public record Item(Integer id, Integer userId, String customerName, String customerEmail,
            Integer orderId, String orderCode, LocalDateTime orderCreatedAt, String orderStatus,
            BigDecimal discountAmount, String status) {}
}
