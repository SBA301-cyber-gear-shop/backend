package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBrandRequest(
    @NotBlank(message = "Tên thương hiệu không được để trống")
    String name,
    String logoUrl,
    String description
) {}
