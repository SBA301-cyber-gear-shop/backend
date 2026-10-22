package com.project.cybershop.catalog.internal.controller;

import com.project.cybershop.catalog.api.dto.request.CreateSpecificationRequest;
import com.project.cybershop.catalog.api.dto.response.SpecificationResponse;
import com.project.cybershop.catalog.internal.service.SpecificationService;
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
@RequestMapping("/api/admin/specifications")
@Tag(name = "Admin - Specifications", description = "Quản lý từ điển thông số kỹ thuật toàn cục")
public class AdminSpecificationController {

    private final SpecificationService specificationService;

    public AdminSpecificationController(SpecificationService specificationService) {
        this.specificationService = specificationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo thông số kỹ thuật mới (VD: Socket, Chuẩn RAM)")
    public ResponseEntity<SpecificationResponse> createSpecification(@Valid @RequestBody CreateSpecificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specificationService.createSpecification(request));
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả thông số kỹ thuật")
    public ResponseEntity<List<SpecificationResponse>> getAllSpecifications() {
        return ResponseEntity.ok(specificationService.getAllSpecifications());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thông số theo ID")
    public ResponseEntity<SpecificationResponse> getSpecificationById(@PathVariable Integer id) {
        return ResponseEntity.ok(specificationService.getSpecificationById(id));
    }

    @PostMapping("/{id}/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm tùy chọn giá trị chuẩn hóa cho thông số (VD: DDR5, LGA1700)")
    public ResponseEntity<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse> addOption(
            @PathVariable Integer id,
            @Valid @RequestBody com.project.cybershop.catalog.api.dto.request.CreateSpecificationOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specificationService.addOption(id, request));
    }

    @GetMapping("/{id}/options")
    @Operation(summary = "Lấy danh sách các tùy chọn giá trị của một thông số")
    public ResponseEntity<List<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse>> getOptions(
            @PathVariable Integer id) {
        return ResponseEntity.ok(specificationService.getOptionsBySpecificationId(id));
    }
}
