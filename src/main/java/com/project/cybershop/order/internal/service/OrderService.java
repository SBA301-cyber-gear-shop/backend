package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.request.*;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse place(String email, String idempotencyKey, PlaceOrderRequest request);
    List<OrderResponse> myOrders(String email);
    OrderResponse myOrder(String email, String code);
    OrderResponse cancel(String email, String code, boolean staff);
    List<OrderResponse> operations(String email, String search, String status);
    OrderResponse advance(String email, String code, String nextStatus);
    OrderResponse collect(String email, String code, CollectPaymentRequest request);
    OrderResponse handover(String email, String code, HandoverRequest request);
}
