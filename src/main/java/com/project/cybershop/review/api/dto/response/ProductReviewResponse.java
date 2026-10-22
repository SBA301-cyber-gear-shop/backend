package com.project.cybershop.review.api.dto.response;

public record ProductReviewResponse(String id, String productId, Integer rating, String comment, String name) {}
