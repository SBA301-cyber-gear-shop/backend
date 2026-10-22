package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.request.CheckoutDetailsRequest;
import com.project.cybershop.order.api.dto.response.CheckoutDetailsResponse;

public interface CheckoutDetailsService {
    CheckoutDetailsResponse preview(String loginEmail, CheckoutDetailsRequest request);
}
