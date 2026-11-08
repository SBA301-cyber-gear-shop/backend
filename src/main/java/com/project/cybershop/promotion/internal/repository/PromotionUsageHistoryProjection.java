package com.project.cybershop.promotion.internal.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PromotionUsageHistoryProjection {
    Integer getId();
    Integer getUserId();
    String getCustomerName();
    String getCustomerEmail();
    Integer getOrderId();
    String getOrderCode();
    LocalDateTime getOrderCreatedAt();
    String getOrderStatus();
    BigDecimal getDiscountAmount();
    String getStatus();
}
