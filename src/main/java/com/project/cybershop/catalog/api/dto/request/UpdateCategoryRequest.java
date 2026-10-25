package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không quá 100 ký tự")
    String name,

    @Size(max = 150, message = "Slug không quá 150 ký tự")
    String slug,

    Integer parentId,

    String description
) {}
