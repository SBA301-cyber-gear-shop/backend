package com.project.cybershop.promotion.api;

import java.math.BigDecimal;

public interface PromotionApi {
    record Quote(Integer promotionId, String code, BigDecimal discount) {}
    /** lock=true is used inside the order transaction to serialize quota allocation. */
    Quote quote(Integer userId, String code, BigDecimal total, boolean lock);
    void reserve(Quote quote, Integer userId, Integer orderId, boolean paid);
    void markUsed(Integer orderId);
    void release(Integer orderId);
    void syncProductPrices(java.util.List<Integer> variantIds);
}
