package com.project.cybershop.support.internal.model;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import java.util.List;

public record ReconstructedBuildContext(
        List<AiChatResponse.ProductSummary> components,
        Long originalBudget,
        String botBuildText
) {}
