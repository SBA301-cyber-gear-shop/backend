package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSpecificationRequest(
    @NotBlank(message = "Tên thông số kỹ thuật không được để trống")
    String name
) {}
