package com.project.cybershop.catalog.api.dto.response;

import java.util.List;

public record ProductResponse(
    Integer id,
    String name,
    Integer brandId,
    String brandName,
    Integer categoryId,
    String categoryName,
    Integer warrantyPeriodMonths,
    String description,
    String status,
    List<ProductVariantResponse> variants
) {}
