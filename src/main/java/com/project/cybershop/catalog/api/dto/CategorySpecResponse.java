package com.project.cybershop.catalog.api.dto;

public record CategorySpecResponse(
    Integer id,
    Integer specificationId,
    String specificationName,
    Boolean isFilterable,
    Boolean isPrimary,
    Boolean showInMenu
) {}
