
package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.ProductResponse;
import com.project.cybershop.order.internal.repository.ProductLookup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductLookup productLookup;

    public ProductService(ProductLookup productLookup) {
        this.productLookup = productLookup;
    }

    // Lấy tất cả phiên bản sản phẩm đang kinh doanh
    public List<ProductResponse> getAllProducts() {
        return productLookup.findAllActive()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Tìm kiếm sản phẩm theo từ khóa
    public List<ProductResponse> searchProducts(String keyword) {
        return productLookup.search(keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Xem chi tiết một phiên bản sản phẩm
    public ProductResponse getProductByVariantId(Integer variantId) {

        if (variantId == null || variantId <= 0) {
            throw new IllegalArgumentException(
                    "ID phiên bản sản phẩm không hợp lệ"
            );
        }

        ProductLookup.ProductInfo product =
                productLookup.findByVariantId(variantId);

        if (product == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy sản phẩm hoặc sản phẩm đã ngừng kinh doanh"
            );
        }

        return toResponse(product);
    }

    // Chuyển dữ liệu từ repository sang DTO
    private ProductResponse toResponse(
            ProductLookup.ProductInfo product
    ) {
        return new ProductResponse(
                product.variantId(),
                product.productId(),
                product.productName(),
                product.variantName(),
                product.price(),
                product.stockQuantity(),
                product.imageUrl(),
                product.warrantyPeriodMonths()
        );
    }
}
