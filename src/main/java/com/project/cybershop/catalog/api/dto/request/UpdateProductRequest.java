package com.project.cybershop.catalog.api.dto.request;

import com.project.cybershop.catalog.internal.entity.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProductRequest(
    @NotBlank(message = "Tên sản phẩm không được để trống")
    String name,
    @NotNull(message = "brandId không được để trống")
    Integer brandId,
    @NotNull(message = "categoryId không được để trống")
    Integer categoryId,
    @NotNull(message = "warrantyPeriodMonths không được để trống")
    @Min(value = 0, message = "Thời gian bảo hành không được âm")
    Integer warrantyPeriodMonths,
    String description,
    ProductStatus status
) {}
