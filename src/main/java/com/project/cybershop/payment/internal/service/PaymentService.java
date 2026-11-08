package com.project.cybershop.payment.internal.service;

import com.project.cybershop.payment.api.PaymentApi;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService extends PaymentApi {
    boolean processVnPayCallback(Map<String, String> params);
    String generateVnPayUrl(String orderCode, BigDecimal amount, String clientIp);
}
