package com.project.cybershop.catalog.internal.controller;

import com.project.cybershop.catalog.api.dto.CategoryResponse;
import com.project.cybershop.catalog.api.dto.CategorySpecResponse;
import com.project.cybershop.catalog.api.dto.ConfigureCategorySpecRequest;
import com.project.cybershop.catalog.api.dto.CreateCategoryRequest;
import com.project.cybershop.catalog.internal.service.CategoryService;
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
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Admin - Categories", description = "Quản lý danh mục và cấu hình thông số kỹ thuật theo danh mục")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo danh mục mới (hỗ trợ phân cấp cha/con)")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PostMapping("/{id}/specifications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cấu hình thông số kỹ thuật cho danh mục (bộ lọc, cấu hình nổi bật, mega menu)")
    public ResponseEntity<List<CategorySpecResponse>> configureCategorySpecifications(
            @PathVariable Integer id,
            @Valid @RequestBody List<ConfigureCategorySpecRequest> requests) {
        return ResponseEntity.ok(categoryService.configureCategorySpecifications(id, requests));
    }

    @GetMapping
    @Operation(summary = "Lấy cây danh mục sản phẩm")
    public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết danh mục theo ID")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
}
