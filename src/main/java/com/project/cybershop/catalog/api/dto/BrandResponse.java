package com.project.cybershop.catalog.api.dto;

public record BrandResponse(
    Integer id,
    String name,
    String logoUrl,
    String description
) {}
