package com.project.cybershop.catalog.api.dto.response;

public record CategorySpecResponse(
    Integer id,
    Integer specificationId,
    String specificationName,
    Boolean isFilterable,
    Boolean isPrimary,
    Boolean showInMenu,
    Boolean isCompatibility
) {}
