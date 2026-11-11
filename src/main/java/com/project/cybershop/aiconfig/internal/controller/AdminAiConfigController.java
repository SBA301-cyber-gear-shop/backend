package com.project.cybershop.aiconfig.internal.controller;

import com.project.cybershop.aiconfig.api.dto.AiConfigDto;
import com.project.cybershop.aiconfig.internal.dto.*;
import com.project.cybershop.aiconfig.internal.service.AiConfigServiceImpl;
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
@RequestMapping("/api/admin/ai-configs")
@RequiredArgsConstructor
@Tag(name = "Admin - AI Configs", description = "Quản lý và tinh chỉnh cấu hình các mô hình AI, API Key và System Prompt")
public class AdminAiConfigController {

    private final AiConfigServiceImpl aiConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy toàn bộ danh sách cấu hình AI trong hệ thống")
    public ResponseEntity<List<AiConfigDto>> getAllConfigs() {
        return ResponseEntity.ok(aiConfigService.getAllConfigs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy chi tiết một cấu hình AI theo ID")
    public ResponseEntity<AiConfigDto> getConfigById(@PathVariable Integer id) {
        return ResponseEntity.ok(aiConfigService.getConfigById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm mới một cấu hình AI")
    public ResponseEntity<AiConfigDto> createConfig(@Valid @RequestBody AiConfigCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiConfigService.createConfig(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin cấu hình AI")
    public ResponseEntity<AiConfigDto> updateConfig(
            @PathVariable Integer id,
            @Valid @RequestBody AiConfigUpdateRequest request) {
        return ResponseEntity.ok(aiConfigService.updateConfig(id, request));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bật/Tắt kích hoạt cấu hình AI (Tự động hoán đổi cấu hình cùng configKey)")
    public ResponseEntity<AiConfigDto> toggleActive(
            @PathVariable Integer id,
            @RequestBody(required = false) AiConfigToggleActiveRequest request) {
        Boolean targetStatus = request != null ? request.getIsActive() : null;
        return ResponseEntity.ok(aiConfigService.toggleActive(id, targetStatus));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa cấu hình AI (Chỉ cho phép xóa cấu hình đang không hoạt động)")
    public ResponseEntity<Void> deleteConfig(@PathVariable Integer id) {
        aiConfigService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chạy thử nghiệm Prompt trong Sandbox test")
    public ResponseEntity<AiTestPromptResponse> testPrompt(@Valid @RequestBody AiTestPromptRequest request) {
        return ResponseEntity.ok(aiConfigService.testPrompt(request));
    }
}

