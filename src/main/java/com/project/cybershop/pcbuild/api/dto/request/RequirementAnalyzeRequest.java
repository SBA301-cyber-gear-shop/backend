package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequirementAnalyzeRequest(
        @NotNull(message = "Ngân sách không được để trống")
        @Positive(message = "Ngân sách phải lớn hơn 0")
        @Max(value = 9_999_999_999L, message = "Ngân sách tối đa là 9.999.999.999đ")
        Long budget,

        @NotBlank(message = "Mô tả nhu cầu không được để trống")
        @Size(max = 500, message = "Mô tả nhu cầu tối đa 500 ký tự")
        String message
) {}

