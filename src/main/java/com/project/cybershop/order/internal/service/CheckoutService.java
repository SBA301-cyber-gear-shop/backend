package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.response.CheckoutPreviewResponse;
import java.util.List;

public interface CheckoutService {
    CheckoutPreviewResponse validate(String loginEmail);
    CheckoutPreviewResponse validate(String loginEmail, List<Integer> itemVariantIds);
}
