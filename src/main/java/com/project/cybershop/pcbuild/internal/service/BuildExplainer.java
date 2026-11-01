package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** AI viết lời giải thích cho bộ máy solver đã chọn (DD 8.4). AI không chọn, thêm hay bớt linh kiện. */
public interface BuildExplainer {

    String PROMPT_VERSION = "EXPLAIN_V3";

    /** @param aiStatus OK, hoặc PARTIAL khi AI trả lý do cho variantId không thuộc bộ máy (dòng đó bị bỏ) */
    record Explanation(String buildName, String aiReasoning, String performanceHighlight,
                       Map<Integer, String> partReasons, String aiStatus) {
    }

    Explanation explain(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt,
                        CompatibilitySnapshot snapshot, Instant deadline);
}
