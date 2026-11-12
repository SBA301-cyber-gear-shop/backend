package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CustomerContext;

import java.util.List;

public interface SupportOrderAndPolicyAdvisor {

    AiChatResponse generateRuleBasedResponse(
            String message,
            String resolvedQuery,
            List<AiChatResponse.ProductSummary> products,
            CustomerContext ctx,
            String knowledgeContext,
            String dynamicOrderContext,
            String conversationId);
}
