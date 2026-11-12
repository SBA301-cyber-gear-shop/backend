package com.project.cybershop.support.internal.model;

import com.project.cybershop.support.api.dto.response.AiChatResponse;

public record ComponentUpgradeDiff(
        String categoryName,
        AiChatResponse.ProductSummary oldItem,
        AiChatResponse.ProductSummary newItem,
        double priceDelta,
        String reason
) {}
