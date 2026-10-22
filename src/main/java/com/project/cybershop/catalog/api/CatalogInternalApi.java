package com.project.cybershop.catalog.api;

import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.api.dto.response.PurchaseVariantResponse;

import java.util.List;
import java.util.Optional;

public interface CatalogInternalApi {
    boolean isVariantInStock(Integer variantId, int requestedQty);
    void deductStock(Integer variantId, int quantity);
    void restoreStock(Integer variantId, int quantity);
    ProductVariantResponse getVariantDetail(Integer variantId);

    void lockVariantsForPurchase(List<Integer> variantIds);

    PurchaseVariantResponse getPurchaseVariant(Integer variantId);

    Optional<PurchaseVariantResponse> findActiveVariant(Integer variantId);
}
