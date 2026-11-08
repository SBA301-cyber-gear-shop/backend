package com.project.cybershop.payment.internal.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnPayConfig {

    @Value("${app.vnpay.tmn-code:3RBH6UL5}")
    private String tmnCode;

    @Value("${app.vnpay.hash-secret:WVWWFTHJALERWKHEOBBIHVXHVRTUYJRL}")
    private String hashSecret;

    @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${app.vnpay.return-url:http://localhost:5173/thanh-toan/ket-qua}")
    private String returnUrl;
}
