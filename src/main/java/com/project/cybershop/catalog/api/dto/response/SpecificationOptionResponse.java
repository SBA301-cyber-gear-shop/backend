package com.project.cybershop.catalog.api.dto.response;

public record SpecificationOptionResponse(
    Integer id,
    Integer specificationId,
    String code,
    String displayValue
) {}
