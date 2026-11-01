package com.project.cybershop.pcbuild.internal.model;

import java.math.BigDecimal;

/**
 * Đầu vào của BuildSolver (DD 6.1).
 *
 * @param profile  hồ sơ đã qua ProfileValidator và đã điền từ hồ sơ mẫu
 * @param pool     ứng viên đã lọc theo hồ sơ, dựng từ snapshot.candidates()
 * @param snapshot cùng snapshot với pool; solver đưa vào EvaluationContext mỗi lần gọi engine
 */
public record SolverInput(BigDecimal budget, ValidatedProfile profile, CandidatePool pool, CompatibilitySnapshot snapshot) {
}
