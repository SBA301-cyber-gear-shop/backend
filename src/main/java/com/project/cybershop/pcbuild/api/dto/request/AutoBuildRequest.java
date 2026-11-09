package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu tự build: budget và purpose lấy từ data.requirement của /requirements/analyze,
 * hoặc purpose khách chọn trong suggestedPurposes. Server kiểm lại, không tin kết quả analyze.
 */
public record AutoBuildRequest(
        @NotNull(message = "Ngân sách không được để trống")
        @Positive(message = "Ngân sách phải lớn hơn 0")
        @Max(value = 9_999_999_999L, message = "Ngân sách tối đa là 9.999.999.999đ")
        Long budget,

        @NotBlank(message = "Mục đích không được để trống")
        @Size(max = 30, message = "Mục đích tối đa 30 ký tự")
        String purpose
) {
}
