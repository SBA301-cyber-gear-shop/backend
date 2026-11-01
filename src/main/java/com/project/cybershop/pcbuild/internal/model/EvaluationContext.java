package com.project.cybershop.pcbuild.internal.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Ngữ cảnh một lần đánh giá (DD 7.1).
 *
 * @param budget   null = bỏ qua kiểm tra ngân sách (J8)
 * @param profile  null = không kiểm lệch hồ sơ; khác null và mode USER thì sinh check P:* mức warning (DD 7.4)
 * @param mode     SOLVER: thiếu dữ liệu = fail; USER: thiếu dữ liệu = unknown
 * @param snapshot bắt buộc: nguồn duy nhất của luật và thông số bắt buộc
 */
public record EvaluationContext(BigDecimal budget, ValidatedProfile profile, Mode mode, CompatibilitySnapshot snapshot) {

    public enum Mode { SOLVER, USER }

    public EvaluationContext {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(snapshot, "snapshot");
    }

    public EvaluationContext(BigDecimal budget, Mode mode, CompatibilitySnapshot snapshot) {
        this(budget, null, mode, snapshot);
    }
}
