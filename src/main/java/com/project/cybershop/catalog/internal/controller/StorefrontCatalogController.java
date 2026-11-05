package com.project.cybershop.catalog.internal.controller;

import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.catalog.api.dto.response.ProductResponse;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.internal.service.CategoryService;
import com.project.cybershop.catalog.internal.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Storefront - Catalog", description = "Các API công khai cho khách hàng duyệt sản phẩm và danh mục")
public class StorefrontCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public StorefrontCatalogController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    @Operation(summary = "Lấy danh sách các sản phẩm đang được kinh doanh (status = ACTIVE, hỗ trợ lọc theo categoryId hoặc categorySlug)")
    public ResponseEntity<List<ProductResponse>> getActiveProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String categorySlug) {
        return ResponseEntity.ok(productService.listActiveProducts(categoryId, categorySlug));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Xem chi tiết sản phẩm và các phiên bản đang bán")
    public ResponseEntity<ProductResponse> getProductDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/products/{id}/variants")
    @Operation(summary = "Lấy danh sách các biến thể của sản phẩm đang bán")
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getVariantsByProductId(id));
    }

    @GetMapping("/products/{id}/variants/{variantId}")
    @Operation(summary = "Xem chi tiết một biến thể đang bán")
    public ResponseEntity<ProductVariantResponse> getVariantDetail(
            @PathVariable Integer id,
            @PathVariable Integer variantId) {
        return ResponseEntity.ok(productService.getVariantById(id, variantId));
    }

    @GetMapping("/categories")
    @Operation(summary = "Lấy menu danh mục sản phẩm (Mega menu kèm cấu hình thông số)")
    public ResponseEntity<List<CategoryResponse>> getStorefrontCategories() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/categories/{slug}")
    @Operation(summary = "Lấy thông tin danh mục theo slug kèm cấu hình thông số")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }
}
