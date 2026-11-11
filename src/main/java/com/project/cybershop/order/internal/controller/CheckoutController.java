package com.project.cybershop.order.internal.controller;

import com.project.cybershop.order.api.dto.request.CheckoutValidateRequest;
import com.project.cybershop.order.api.dto.response.CheckoutPreviewResponse;
import com.project.cybershop.order.internal.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public CheckoutPreviewResponse validate(
            Authentication authentication,
            @RequestBody(required = false) CheckoutValidateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        List<Integer> cartItemIds = request != null ? request.cartItemIds() : null;
        List<Integer> itemVariantIds = request != null ? request.itemVariantIds() : null;
        return checkoutService.validate(authentication.getName(), cartItemIds, itemVariantIds);
    }
}
