package com.project.cybershop.order.internal.service;

public interface CartService {

    /**
     * Thêm một phiên bản sản phẩm vào giỏ hàng.
     *
     * @param userId người dùng đang đăng nhập
     * @param productVariantId ID phiên bản sản phẩm
     * @param quantity số lượng muốn thêm
     */
    void addToCart(Integer userId, Integer productVariantId, Integer quantity);

}

