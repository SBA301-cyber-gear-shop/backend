package com.project.cybershop.shipping.internal.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GhnConfig {

    @Value("${app.shipping.ghn.base-url:https://online-gateway.ghn.vn/shiip/public-api}")
    private String baseUrl;

    @Value("${app.shipping.ghn.token:a32a31f1-1f12-4fee-ae61-3573429ca09a}")
    private String token;

    @Value("${app.shipping.ghn.shop-id:3036873}")
    private Integer shopId;

    @Value("${app.shipping.ghn.from-district-id:1454}")
    private Integer fromDistrictId;

    @Value("${app.shipping.ghn.from-ward-code:21210}")
    private String fromWardCode;

    @Value("${app.shipping.ghn.from-name:CyberShop Store}")
    private String fromName;

    @Value("${app.shipping.ghn.from-phone:0877771230}")
    private String fromPhone;

    @Value("${app.shipping.ghn.from-address:Hẻm 282 Lê Thị Riêng, P. Thới An, Quận 12, TP.HCM}")
    private String fromAddress;
}
