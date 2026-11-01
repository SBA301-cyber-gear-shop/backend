package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;

import java.math.BigDecimal;

/** Kiểm đầu vào /ai-suggest (DD 5.7); vi phạm ném BuildRuleException 400 với mã ổn định. */
public interface RequestValidator {

    /** @param floorPrice giá bộ rẻ nhất hợp lệ của snapshot hiện tại (DD 6.8); null nếu kho không ráp được bộ nào */
    void validate(AiBuildRequest request, BigDecimal floorPrice);

    BigDecimal minBudget();

    BigDecimal maxBudget();
}
