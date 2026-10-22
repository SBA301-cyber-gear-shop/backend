package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.response.CheckoutPreviewResponse;

public interface CheckoutService {
    CheckoutPreviewResponse validate(String loginEmail);
}
