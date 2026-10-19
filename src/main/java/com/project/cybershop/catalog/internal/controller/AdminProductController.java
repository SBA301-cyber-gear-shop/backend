package com.project.cybershop.catalog.internal.controller;

import com.project.cybershop.catalog.api.dto.request.CreateProductRequest;
import com.project.cybershop.catalog.api.dto.request.CreateVariantDto;
import com.project.cybershop.catalog.api.dto.response.ProductResponse;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.internal.entity.ProductStatus;
import com.project.cybershop.catalog.internal.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin - Products", description = "Quản lý sản phẩm: tạo bản nháp, thêm biến thể SKU, gán thông số EAV và đăng bán")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo sản phẩm mới (Lưu bản nháp - DRAFT, kèm biến thể ban đầu nếu có)")
    public ResponseEntity<ProductResponse> createProductDraft(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProductDraft(request));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm biến thể / SKU mới vào sản phẩm đã có kèm thông số kỹ thuật")
    public ResponseEntity<ProductVariantResponse> addVariant(
            @PathVariable Integer id,
            @Valid @RequestBody CreateVariantDto variantDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addVariant(id, variantDto));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kiểm tra tính hợp lệ và Đăng bán sản phẩm (Chuyển DRAFT -> ACTIVE)")
    public ResponseEntity<ProductResponse> publishProduct(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.publishProduct(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết sản phẩm dành cho Admin (Bao gồm cả bản nháp)")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm theo trạng thái (DRAFT, ACTIVE, ARCHIVED)")
    public ResponseEntity<List<ProductResponse>> listProducts(
            @RequestParam(required = false) ProductStatus status) {
        if (status != null) {
            return ResponseEntity.ok(productService.listProductsByStatus(status));
        }
        return ResponseEntity.ok(productService.listProductsByStatus(ProductStatus.DRAFT));
    }
}
