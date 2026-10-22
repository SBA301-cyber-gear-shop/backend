package com.project.cybershop.review.internal.repository;

public interface ProductReviewProjection {

    String getId();

    String getProductId();

    Integer getRating();

    String getComment();

    String getName();
}
