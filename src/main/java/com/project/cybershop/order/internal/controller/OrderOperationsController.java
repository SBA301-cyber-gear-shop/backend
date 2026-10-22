package com.project.cybershop.order.internal.controller;

import com.project.cybershop.order.api.dto.request.*;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import com.project.cybershop.order.internal.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/operations/orders")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class OrderOperationsController {
    private final OrderService service;
    public OrderOperationsController(OrderService service) { this.service = service; }

    @GetMapping
    public List<OrderResponse> list(Authentication auth, @RequestParam(required=false) String search,
                                     @RequestParam(required=false) String status) {
        return service.operations(auth.getName(), search, status);
    }

    public record StatusRequest(@NotBlank @Size(max=30) String status) {}

    @PatchMapping("/{code}/status")
    public OrderResponse advance(Authentication auth, @PathVariable String code, @Valid @RequestBody StatusRequest request) {
        return service.advance(auth.getName(), code, request.status());
    }

    @PostMapping("/{code}/collect")
    public OrderResponse collect(Authentication auth, @PathVariable String code, @Valid @RequestBody CollectPaymentRequest request) {
        return service.collect(auth.getName(), code, request);
    }

    @PostMapping("/{code}/handover")
    public OrderResponse handover(Authentication auth, @PathVariable String code, @Valid @RequestBody HandoverRequest request) {
        return service.handover(auth.getName(), code, request);
    }

    @PostMapping("/{code}/cancel")
    public OrderResponse cancel(Authentication auth, @PathVariable String code) { return service.cancel(auth.getName(), code, true); }
}
