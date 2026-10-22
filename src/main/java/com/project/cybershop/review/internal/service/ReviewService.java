package com.project.cybershop.review.internal.service;

import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ProductReviewResponse;
import com.project.cybershop.review.api.ReviewApi;
import java.util.List;

public interface ReviewService extends ReviewApi {
    void save(String email, Integer orderItemId, ReviewRequest request);
    List<ProductReviewResponse> forVariant(Integer variantId);
}
