package com.project.cybershop.order.internal.repository;

import java.math.BigDecimal;

public interface CheckoutItemProjection {

    Integer getCartItemId();
    Integer getProductVariantId();
    Integer getExistingVariantId();
    Integer getExistingProductId();
    Integer getQuantity();
    String getProductName();
    String getProductStatus();
    String getVariantName();
    String getImageUrl();
    BigDecimal getUnitPrice();
    Integer getStockQuantity();
    Integer getPcCustomId();
    String getPcCustomName();
}
