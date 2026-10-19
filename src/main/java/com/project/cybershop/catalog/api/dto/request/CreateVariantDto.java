package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateVariantDto(
    @NotBlank(message = "Mã serial_code (SKU) không được để trống")
    String serialCode,
    String variantName,
    String slug,
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được âm")
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stockQuantity,
    String imageUrl,
    List<AttributeValueInputDto> attributes
) {}
