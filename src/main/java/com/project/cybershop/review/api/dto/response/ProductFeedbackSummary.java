package com.project.cybershop.review.api.dto.response;

import java.math.BigDecimal;

public record ProductFeedbackSummary(
        Integer productVariantId,
        BigDecimal averageRating,
        long reviewCount,
        long buyerCount
) {}
