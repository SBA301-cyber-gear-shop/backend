package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;

import java.util.List;

/**
 * Kết quả bước 6.
 *
 * @param difference tổng trừ ngân sách, âm là còn dư
 */
public record BuildCheck(BuildStatus status, long total, long difference, List<RuleViolation> violations) {
}
