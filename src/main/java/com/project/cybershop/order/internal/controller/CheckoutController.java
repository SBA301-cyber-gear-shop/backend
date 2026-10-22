package com.project.cybershop.order.internal.controller;

import com.project.cybershop.order.api.dto.response.CheckoutPreviewResponse;
import com.project.cybershop.order.internal.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public CheckoutPreviewResponse validate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        // Assumption: authentication.getName() is the user's email.
        // Adapt this one line if your JWT stores user ID/username instead.
        return checkoutService.validate(authentication.getName());
    }
}
