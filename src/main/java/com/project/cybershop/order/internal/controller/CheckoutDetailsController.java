package com.project.cybershop.order.internal.controller;

import com.project.cybershop.order.api.dto.request.CheckoutDetailsRequest;
import com.project.cybershop.order.api.dto.response.CheckoutDetailsResponse;
import com.project.cybershop.order.internal.service.CheckoutDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutDetailsController {
    private final CheckoutDetailsService service;

    public CheckoutDetailsController(CheckoutDetailsService service) {
        this.service = service;
    }

    @PostMapping("/details/preview")
    public CheckoutDetailsResponse preview(@RequestBody CheckoutDetailsRequest request,
                                           Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        // Giống bước 4: giả định getName() trả email.
        return service.preview(authentication.getName(), request);
    }
}
