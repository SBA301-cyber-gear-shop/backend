package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;

import java.util.List;

/**
 * Nơi duy nhất quyết định tương thích (DD 7). Thuần bộ nhớ: mọi dữ liệu (luật, thông số bắt buộc,
 * spec đã phân tích) lấy từ ctx.snapshot(), không gọi DB, HTTP hay LLM.
 */
public interface CompatibilityEngine {
    EngineReport evaluate(List<BuildLine> lines, EvaluationContext ctx);
}
