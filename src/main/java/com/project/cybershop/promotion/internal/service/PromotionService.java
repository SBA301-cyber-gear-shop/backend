package com.project.cybershop.promotion.internal.service;

import com.project.cybershop.promotion.api.PromotionApi;
import com.project.cybershop.promotion.api.dto.request.PromotionRequest;
import com.project.cybershop.promotion.api.dto.response.PromotionResponse;
import java.util.List;

public interface PromotionService extends PromotionApi {
    List<PromotionResponse> list(String email);
    PromotionResponse save(String email, Integer id, PromotionRequest request);
    List<PromotionResponse> publicList();
}
