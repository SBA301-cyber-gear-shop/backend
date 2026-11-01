package com.project.cybershop.pcbuild.internal.model;

/** Bản sao bất biến của một dòng compatibility_rules (không phải entity JPA, tránh lazy loading ngoài transaction). */
public record RuleDefinition(int id, String ruleName, int category1Id, int spec1Id,
                             int category2Id, int spec2Id, String operator) {
}
