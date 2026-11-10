package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.request.AddToCartRequest;
import com.project.cybershop.order.api.dto.request.UpdateCartItemRequest;
import com.project.cybershop.order.api.dto.response.CartResponse;

import java.util.List;

public interface CartService {
    void addToCart(Integer userId, AddToCartRequest request);
    void addBatchToCart(Integer userId, List<AddToCartRequest> items);
    void addBatchToCart(Integer userId, List<AddToCartRequest> items, Integer pcCustomId);
    CartResponse getMyCart(Integer userId);
    void updateCartItem(Integer userId, Integer cartItemId, UpdateCartItemRequest request);
    void removeCartItem(Integer userId, Integer cartItemId);
    void removePcSet(Integer userId, Integer pcCustomId);
    void detachPcSet(Integer userId, Integer pcCustomId);
}
