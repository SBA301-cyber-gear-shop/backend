package com.project.cybershop.catalog.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantResponse(
    Integer id,
    Integer productId,
    String serialCode,
    String variantName,
    String slug,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stockQuantity,
    String imageUrl,
    List<SpecificationValueResponse> attributes
) {}
