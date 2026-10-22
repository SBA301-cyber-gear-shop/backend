package com.project.cybershop.review.internal.repository;

public interface ProductFeedbackProjection {

    Integer getProductVariantId();

    Long getRatingTotal();

    Long getReviewCount();

    Long getBuyerCount();
}
