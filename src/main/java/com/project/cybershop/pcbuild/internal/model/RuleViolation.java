package com.project.cybershop.pcbuild.internal.model;

/**
 * Một lỗi tương thích hoặc lỗi khuôn.
 *
 * @param rule RULE_&lt;id DB&gt;, PSU_POWER, MISSING_SLOT, NOT_IN_TEMPLATE, DUPLICATE_SLOT hoặc QTY_RANGE
 */
public record RuleViolation(String rule, String message) {
}
