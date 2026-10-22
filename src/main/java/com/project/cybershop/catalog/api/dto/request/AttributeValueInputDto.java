package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record AttributeValueInputDto(
    @NotNull(message = "specificationId không được để trống")
    Integer specificationId,
    Integer optionId,
    String value
) {}
