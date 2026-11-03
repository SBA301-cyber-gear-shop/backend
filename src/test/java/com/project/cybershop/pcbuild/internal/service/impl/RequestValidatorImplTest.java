package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestValidator - fullSetBudget của chế độ Trọn bộ (B4)")
class RequestValidatorImplTest {

    private final RequestValidatorImpl validator = new RequestValidatorImpl(new ObjectMapper(),
            BigDecimal.valueOf(8_000_000), new BigDecimal("9999999999"));

    private static AiBuildRequest request(long budget, Long fullSetBudget) {
        return AiBuildRequest.builder().budget(BigDecimal.valueOf(budget)).purpose("Chơi game")
                .fullSetBudget(fullSetBudget != null ? BigDecimal.valueOf(fullSetBudget) : null).build();
    }

    @Test
    @DisplayName("không gửi hoặc gửi hợp lệ (lớn hơn hay bằng ngân sách thùng máy) -> qua")
    void acceptsValidFullSetBudget() {
        assertThatCode(() -> validator.validate(request(24_000_000, null), null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(request(24_000_000, 30_000_000L), null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(request(24_000_000, 24_000_000L), null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nhỏ hơn ngân sách thùng máy -> 400 INVALID_REQUEST")
    void rejectsSmallerThanPcBudget() {
        assertThatThrownBy(() -> validator.validate(request(24_000_000, 23_999_999L), null))
                .isInstanceOfSatisfying(BuildRuleException.class, error -> assertThat(error.getCode()).isEqualTo("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("vượt ngân sách tối đa -> 400 BUDGET_TOO_HIGH")
    void rejectsAboveMax() {
        assertThatThrownBy(() -> validator.validate(request(24_000_000, 10_000_000_000L), null))
                .isInstanceOfSatisfying(BuildRuleException.class, error -> assertThat(error.getCode()).isEqualTo("BUDGET_TOO_HIGH"));
    }
}
