package com.project.cybershop.catalog.api.dto.response;

public record SpecificationValueResponse(
    Integer id,
    Integer specificationId,
    String specificationName,
    String value
) {}
