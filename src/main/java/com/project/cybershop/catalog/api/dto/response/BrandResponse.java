package com.project.cybershop.catalog.api.dto.response;

public record BrandResponse(
    Integer id,
    String name,
    String logoUrl,
    String description
) {}
