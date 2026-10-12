package com.project.cybershop.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
    @NotBlank(message = "Tên danh mục không được để trống")
    String name,
    String slug,
    Integer parentId,
    String description
) {}
