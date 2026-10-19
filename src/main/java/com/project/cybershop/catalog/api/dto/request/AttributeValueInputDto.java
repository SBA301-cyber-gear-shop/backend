package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttributeValueInputDto(
    @NotNull(message = "specificationId không được để trống")
    Integer specificationId,
    @NotBlank(message = "Giá trị thông số (value) không được để trống")
    String value
) {}
