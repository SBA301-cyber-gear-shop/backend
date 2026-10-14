package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateProductRequest(
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
    @Valid
    List<CreateVariantDto> variants
) {}
