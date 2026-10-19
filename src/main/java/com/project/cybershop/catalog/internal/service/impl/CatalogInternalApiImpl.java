package com.project.cybershop.catalog.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogInternalApi;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.internal.entity.ProductVariant;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.ProductVariantRepository;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));

        int currentStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        if (currentStock < quantity) {
            throw new BusinessException("Sản phẩm '" + variant.getVariantName() + "' không đủ số lượng tồn kho (Còn: " + currentStock + ", Cần: " + quantity + ")");
        }
        variant.setStockQuantity(currentStock - quantity);
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional
    public void restoreStock(Integer variantId, int quantity) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));

        int currentStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        variant.setStockQuantity(currentStock + quantity);
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantDetail(Integer variantId) {
        ProductVariant variant = productVariantRepository.findWithAttributesById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + variantId));
        return catalogMapper.toVariantResponse(variant);
    }
}
