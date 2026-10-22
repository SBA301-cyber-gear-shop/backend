package com.project.cybershop.catalog.api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Một dòng tương ứng một phiên bản có thể mua, không phải một sản phẩm gộp. */
public record CatalogProductResponse(
        String id, String productId, String name, String category, String categoryName,
        String brand, String sku, String variant, BigDecimal price, BigDecimal originalPrice,
        int stock, int warranty, String image, String description, String status,
        Map<String, String> specs, List<String> primarySpecs, List<String> filterSpecs,
        BigDecimal averageRating, long reviewCount, long buyerCount
) {}
