package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.response.AiChatResponse;

import java.util.List;

public interface SupportProductAdvisor {

    AiChatResponse buildSmartProductAdvisory(
            String message,
            String resolvedQuery,
            List<AiChatResponse.ProductSummary> products,
            String conversationId);

    String buildExpertHardwareInsightForProduct(AiChatResponse.ProductSummary p);

    AiChatResponse buildCompatibilityAdvisory(String message, String conversationId);

    AiChatResponse buildComparisonAdvisory(
            String message,
            List<AiChatResponse.ProductSummary> products,
            String conversationId);
}
