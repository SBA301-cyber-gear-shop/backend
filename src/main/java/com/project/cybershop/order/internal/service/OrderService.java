package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.request.CollectPaymentRequest;
import com.project.cybershop.order.api.dto.request.HandoverRequest;
import com.project.cybershop.order.api.dto.request.PlaceOrderRequest;
import com.project.cybershop.order.api.dto.response.CashCollectionResponse;
import com.project.cybershop.order.api.dto.response.OrderResponse;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    // Đặt hàng, kiểm tra lại giá/tồn/voucher và chống tạo trùng đơn.
    OrderResponse place(String email, String idempotencyKey, PlaceOrderRequest request);

    List<OrderResponse> myOrders(String email);

    OrderResponse myOrder(String email, String orderCode);

    OrderResponse cancel(String email, String orderCode, boolean staff, String reason);

    List<OrderResponse> operations(String email, String search, String status);

    OrderResponse advance(String email, String orderCode, String nextStatus);

    OrderResponse collect(String email, String orderCode, CollectPaymentRequest request);
    Optional<CashCollectionResponse> cashCollection(String email, String orderCode);

    OrderResponse handover(String email, String orderCode, HandoverRequest request);
}
