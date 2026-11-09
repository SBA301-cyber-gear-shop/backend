package com.project.cybershop.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentApi {
    record Payment(String status, String methodType, BigDecimal amount, LocalDateTime paidAt) {}
    void createCounterPayment(Integer orderId, BigDecimal amount);
    void createCodPayment(Integer orderId, BigDecimal amount);
    String createVnPayPayment(Integer orderId, String orderCode, BigDecimal amount, String clientIp);
    Payment get(Integer orderId);
    void collect(Integer orderId, BigDecimal receivedAmount, String reference);
    void cancel(Integer orderId);
}
