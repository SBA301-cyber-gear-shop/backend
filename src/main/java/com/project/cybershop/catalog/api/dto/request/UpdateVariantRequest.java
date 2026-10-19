package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdateVariantRequest(
    @NotBlank(message = "Mã SKU (serial_code) không được để trống")
    String serialCode,
    String variantName,
    String slug,
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
    BigDecimal price,
    BigDecimal originalPrice,
    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    Integer stockQuantity,
    String imageUrl,
    List<AttributeValueInputDto> attributes
) {}
