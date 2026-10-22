package com.project.cybershop.catalog.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogInternalApi;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.api.dto.response.PurchaseVariantResponse;
import com.project.cybershop.catalog.internal.repository.PurchaseVariantProjection;
import com.project.cybershop.catalog.internal.entity.ProductVariant;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.ProductVariantRepository;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogInternalApiImpl implements CatalogInternalApi {

    private final ProductVariantRepository productVariantRepository;
    private final CatalogMapper catalogMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean isVariantInStock(Integer variantId, int requestedQty) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null) return false;
        return variant.getStockQuantity() != null && variant.getStockQuantity() >= requestedQty;
    }

    @Override
    @Transactional
    public void deductStock(Integer variantId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Số lượng trừ tồn phải lớn hơn 0.");
        }

        if (productVariantRepository.deductAvailableStock(variantId, quantity) != 1) {
            throw new BusinessException("Không đủ tồn kho cho phiên bản ID: " + variantId);
        }
    }

    @Override
    @Transactional
    public void restoreStock(Integer variantId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Số lượng hoàn tồn phải lớn hơn 0.");
        }

        if (productVariantRepository.restoreAvailableStock(variantId, quantity) != 1) {
            throw new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId);
        }
    }

    @Override
    @Transactional
    public void lockVariantsForPurchase(List<Integer> variantIds) {
        // Khóa theo cùng thứ tự ID để các đơn nhiều sản phẩm không khóa chéo nhau.
        for (Integer variantId : variantIds.stream().distinct().sorted().toList()) {
            productVariantRepository.lockForPurchase(variantId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseVariantResponse getPurchaseVariant(Integer variantId) {
        PurchaseVariantProjection product = productVariantRepository.findPurchaseVariant(variantId)
                .orElseThrow(() -> new BusinessException("Phiên bản sản phẩm không còn tồn tại."));

        return toPurchaseVariantResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PurchaseVariantResponse> findActiveVariant(Integer variantId) {
        return productVariantRepository.findPurchaseVariant(variantId)
                .filter(product -> "active".equalsIgnoreCase(product.getStatus()))
                .map(this::toPurchaseVariantResponse);
    }

    private PurchaseVariantResponse toPurchaseVariantResponse(PurchaseVariantProjection product) {
        return new PurchaseVariantResponse(
                product.getVariantId(),
                product.getProductName(),
                product.getVariantName(),
                product.getPrice(),
                product.getWarrantyPeriodMonths() == null ? 0 : product.getWarrantyPeriodMonths(),
                product.getStatus(),
                product.getStockQuantity() == null ? 0 : product.getStockQuantity(),
                product.getImageUrl()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantDetail(Integer variantId) {
        ProductVariant variant = productVariantRepository.findWithAttributesById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));
        return catalogMapper.toVariantResponse(variant);
    }
}
