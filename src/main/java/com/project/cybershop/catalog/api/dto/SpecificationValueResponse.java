package com.project.cybershop.catalog.api.dto;

public record SpecificationValueResponse(
    Integer id,
    Integer specificationId,
    String specificationName,
    String value
) {}
