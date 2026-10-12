package com.project.cybershop.catalog.api;

import com.project.cybershop.catalog.api.dto.ProductVariantResponse;

public interface CatalogInternalApi {
    boolean isVariantInStock(Integer variantId, int requestedQty);
    void deductStock(Integer variantId, int quantity);
    void restoreStock(Integer variantId, int quantity);
    ProductVariantResponse getVariantDetail(Integer variantId);
}
