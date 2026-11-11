package com.project.cybershop.order.internal.repository;

public interface CartLineProjection {

    Integer getCartItemId();

    Integer getProductVariantId();

    Integer getQuantity();

    Integer getPcCustomId();
}
