package com.project.cybershop.catalog.internal.repository;

import java.math.BigDecimal;

public interface PurchaseVariantProjection {

    Integer getVariantId();
    String getProductName();
    String getVariantName();
    BigDecimal getPrice();
    Integer getWarrantyPeriodMonths();
    String getStatus();
    Integer getStockQuantity();
    String getImageUrl();
}
