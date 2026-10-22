package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSpecificationOptionRequest(
    @NotBlank(message = "Mã giá trị (code) không được để trống")
    String code,
    @NotBlank(message = "Tên hiển thị (displayValue) không được để trống")
    String displayValue
) {}
