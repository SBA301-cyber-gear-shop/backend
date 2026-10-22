package com.project.cybershop.catalog.api.dto.response;

public record SpecificationValueResponse(
    Integer id,
    Integer specificationId,
    String specificationName,
    Integer optionId,
    String optionCode,
    String value
) {}
