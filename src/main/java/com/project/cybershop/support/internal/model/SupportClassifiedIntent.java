package com.project.cybershop.support.internal.model;

import java.util.List;

/**
 * Kết quả phân loại Ý định & Thực thể có cấu trúc (Structured Output JSON Schema)
 * từ tầng Hybrid LLM Intent Router của CyberBot.
 */
public record SupportClassifiedIntent(
        String intent,
        Long budgetVnd,
        Boolean isAddingExtraBudget,
        List<String> targetCategories,
        List<String> mentionedProducts,
        String pcProfile,
        Boolean preferWhite,
        Boolean preferAesthetic
) {
    public static SupportClassifiedIntent empty() {
        return new SupportClassifiedIntent("UNKNOWN", null, false, List.of(), List.of(), null, false, false);
    }

    public boolean isValidIntent() {
        return intent != null && !intent.isBlank() && !"UNKNOWN".equalsIgnoreCase(intent);
    }
}
