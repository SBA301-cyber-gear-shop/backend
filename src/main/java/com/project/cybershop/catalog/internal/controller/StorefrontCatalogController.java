package com.project.cybershop.catalog.internal.controller;

import com.project.cybershop.catalog.api.dto.CategoryResponse;
import com.project.cybershop.catalog.api.dto.ProductResponse;
import com.project.cybershop.catalog.internal.service.CategoryService;
import com.project.cybershop.catalog.internal.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Storefront - Catalog", description = "Các API công khai cho khách hàng duyệt sản phẩm và danh mục")
public class StorefrontCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/products")
    @Operation(summary = "Lấy danh sách các sản phẩm đang được kinh doanh (status = ACTIVE)")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        return ResponseEntity.ok(productService.listActiveProducts());
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Xem chi tiết sản phẩm và các phiên bản đang bán")
    public ResponseEntity<ProductResponse> getProductDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/categories")
    @Operation(summary = "Lấy menu danh mục sản phẩm (Mega menu kèm cấu hình thông số)")
    public ResponseEntity<List<CategoryResponse>> getStorefrontCategories() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }
}
