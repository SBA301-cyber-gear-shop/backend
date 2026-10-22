package com.project.cybershop.order.internal.repository;

import java.math.BigDecimal;

public interface OrderItemProjection {

    Integer getId();
    Integer getProductVariantId();
    String getProductName();
    Integer getQuantity();
    BigDecimal getUnitPrice();
    BigDecimal getSubtotal();
    Integer getWarrantyPeriodMonths();
    Integer getRating();
    String getReviewComment();
}
