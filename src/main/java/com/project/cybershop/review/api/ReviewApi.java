package com.project.cybershop.review.api;

import com.project.cybershop.review.api.dto.response.ProductFeedbackSummary;

import java.util.Map;

public interface ReviewApi {

    Map<Integer, ProductFeedbackSummary> productSummaries();
}
