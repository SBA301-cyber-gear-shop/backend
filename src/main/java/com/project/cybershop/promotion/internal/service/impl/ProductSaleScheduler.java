package com.project.cybershop.promotion.internal.service.impl;
import com.project.cybershop.promotion.internal.service.ProductSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name = "app.product-sales.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ProductSaleScheduler {
    private final ProductSaleService sales;
    @Scheduled(fixedDelayString = "${app.product-sales.refresh-ms:300000}", initialDelayString = "${app.product-sales.initial-delay-ms:5000}")
    public void refresh() { sales.refreshAll(); }
}
