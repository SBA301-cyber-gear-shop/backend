package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.Promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Scalar projection: a preview must not cache an entity before checkout acquires its lock. */
public record PromotionTerms(
        Integer id,
        String code,
        String discountType,
        BigDecimal discountValue,
        Integer usageLimit,
        Boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public static PromotionTerms from(Promotion promotion) {
        return new PromotionTerms(
                promotion.getId(), promotion.getCode(), promotion.getDiscountType(),
                promotion.getDiscountValue(), promotion.getUsageLimit(), promotion.getIsActive(),
                promotion.getStartDate(), promotion.getEndDate()
        );
    }
}
