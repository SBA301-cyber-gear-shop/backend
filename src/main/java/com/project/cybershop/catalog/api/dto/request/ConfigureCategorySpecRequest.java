package com.project.cybershop.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConfigureCategorySpecRequest(
    @NotNull(message = "specificationId không được để trống")
    Integer specificationId,
    Boolean isFilterable,
    Boolean isPrimary,
    Boolean showInMenu
) {}
