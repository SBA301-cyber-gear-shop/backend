package com.project.cybershop.order.internal.controller;

import com.project.cybershop.order.api.dto.request.PlaceOrderRequest;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import com.project.cybershop.order.internal.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(Authentication auth, @RequestHeader("Idempotency-Key") String key,
                               @Valid @RequestBody PlaceOrderRequest request) {
        return service.place(auth.getName(), key, request);
    }

    @GetMapping
    public List<OrderResponse> mine(Authentication auth) { return service.myOrders(auth.getName()); }

    @GetMapping("/{code}")
    public OrderResponse one(Authentication auth, @PathVariable String code) { return service.myOrder(auth.getName(), code); }

    @PostMapping("/{code}/cancel")
    public OrderResponse cancel(Authentication auth, @PathVariable String code) { return service.cancel(auth.getName(), code, false); }
}
