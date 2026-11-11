package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.pcbuild.internal.dto.admin.*;
import com.project.cybershop.pcbuild.internal.service.AdminPcBuildMetadataService;
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
@RequestMapping("/api/admin/pc-build/metadata")
@RequiredArgsConstructor
@Tag(name = "Admin - PC Build Metadata", description = "Quản lý khuôn linh kiện và hồ sơ ngân sách cho AI PC Builder")
public class AdminPcBuildMetadataController {

    private final AdminPcBuildMetadataService metadataService;

    // =========================================================================
    // TEMPLATE SLOTS
    // =========================================================================

    @GetMapping("/slots")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách các slot linh kiện theo thứ tự lắp ráp")
    public ResponseEntity<List<TemplateSlotDto>> getAllSlots() {
        return ResponseEntity.ok(metadataService.getAllSlots());
    }

    @PostMapping("/slots")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm mới một slot linh kiện")
    public ResponseEntity<TemplateSlotDto> createSlot(@Valid @RequestBody TemplateSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metadataService.createSlot(request));
    }

    @PutMapping("/slots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật cấu hình slot linh kiện")
    public ResponseEntity<TemplateSlotDto> updateSlot(@PathVariable Integer id, @Valid @RequestBody TemplateSlotRequest request) {
        return ResponseEntity.ok(metadataService.updateSlot(id, request));
    }

    @DeleteMapping("/slots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa một slot linh kiện")
    public ResponseEntity<Void> deleteSlot(@PathVariable Integer id) {
        metadataService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/slots/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật lại toàn bộ thứ tự (step_order) của các slot linh kiện theo danh sách ID")
    public ResponseEntity<List<TemplateSlotDto>> reorderSlots(@RequestBody List<Integer> orderedSlotIds) {
        return ResponseEntity.ok(metadataService.reorderSlots(orderedSlotIds));
    }

    @GetMapping("/categories-available")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách category và options phục vụ form tạo slot")
    public ResponseEntity<MetadataCategoryOptionDto> getAvailableOptions() {
        return ResponseEntity.ok(metadataService.getAvailableCategoryOptions());
    }

    // =========================================================================
    // BUDGET PROFILES
    // =========================================================================

    @GetMapping("/budget-profiles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy toàn bộ hồ sơ ngân sách nhóm theo từng mục đích")
    public ResponseEntity<List<BudgetProfilePurposeDto>> getBudgetProfiles() {
        return ResponseEntity.ok(metadataService.getAllBudgetProfilesGrouped());
    }

    @PostMapping("/budget-profiles/purposes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới một mục đích cấu hình ngân sách")
    public ResponseEntity<BudgetProfilePurposeDto> createPurpose(@Valid @RequestBody BudgetProfileCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metadataService.createPurpose(request));
    }

    @PutMapping("/budget-profiles/purposes/{purpose}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật ngân sách sàn và tỷ lệ phân bổ của mục đích")
    public ResponseEntity<BudgetProfilePurposeDto> updatePurpose(
            @PathVariable String purpose,
            @Valid @RequestBody BudgetProfileUpdateRequest request) {
        return ResponseEntity.ok(metadataService.updatePurpose(purpose, request));
    }

    @DeleteMapping("/budget-profiles/purposes/{purpose}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa một mục đích khỏi hồ sơ ngân sách")
    public ResponseEntity<Void> deletePurpose(@PathVariable String purpose) {
        metadataService.deletePurpose(purpose);
        return ResponseEntity.noContent().build();
    }
}

