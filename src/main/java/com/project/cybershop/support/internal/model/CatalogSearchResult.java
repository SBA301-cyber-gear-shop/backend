package com.project.cybershop.support.internal.model;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import java.util.List;

public record CatalogSearchResult(
        String textContext,
        List<AiChatResponse.ProductSummary> products
) {}
