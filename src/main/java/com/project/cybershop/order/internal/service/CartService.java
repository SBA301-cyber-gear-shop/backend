package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.request.AddToCartRequest;
import com.project.cybershop.order.api.dto.request.UpdateCartItemRequest;
import com.project.cybershop.order.api.dto.response.CartResponse;

import java.util.List;

public interface CartService {
    void addToCart(Integer userId, AddToCartRequest request);
    void addBatchToCart(Integer userId, List<AddToCartRequest> items);
    CartResponse getMyCart(Integer userId);
    void updateCartItem(Integer userId, Integer cartItemId, UpdateCartItemRequest request);
    void removeCartItem(Integer userId, Integer cartItemId);
}
