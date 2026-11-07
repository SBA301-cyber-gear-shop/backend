package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.internal.service.PcBuildService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bước 4–6 của AI Build PC: tự build và kiểm tra bộ PC. Không gọi AI nên không qua rate limit AI.
 */
@Tag(name = "AI PC Builder – bước 4–6", description = "Chia ngân sách, chọn linh kiện, kiểm tra tương thích")
@RestController
@RequestMapping("/api/pc-build/builds")
@RequiredArgsConstructor
public class PcBuildController {

    private final PcBuildService pcBuildService;

    @Operation(summary = "Tự build bộ PC theo ngân sách và mục đích",
            description = """
                    Lấy `budget` và `purpose` từ `data.requirement` của /api/pc-build/requirements/analyze
                    (trạng thái READY hoặc BUDGET_WARNING), hoặc purpose khách chọn trong `suggestedPurposes`.
                    Trả bộ PC VALID theo step_order. Lỗi: 400 PURPOSE_INVALID / BUDGET_TOO_LOW / BUDGET_TOO_HIGH,
                    422 BUILD_NOT_FOUND (kho không ráp được, `data.slot` là loại bị vướng), 503 METADATA_INVALID.""")
    @PostMapping("/auto")
    public ApiResponseDTO<BuildResultResponse> autoBuild(@Valid @RequestBody AutoBuildRequest request) {
        return ApiResponseDTO.success(pcBuildService.autoBuild(request));
    }

    @Operation(summary = "Kiểm tra lại bộ PC sau khi khách đổi món",
            description = """
                    Nhận ngân sách và danh sách {variantId, qty}. Trả `status` VALID / INCOMPATIBLE (xung đột phần cứng,
                    xem `violations`) / OVER_BUDGET (chỉ vượt tiền, xem `budgetWarning`), kèm `maxQty` thật của từng món.
                    Lỗi: 400 VARIANT_NOT_FOUND khi id không tồn tại.""")
    @PostMapping("/validate")
    public ApiResponseDTO<BuildResultResponse> validate(@Valid @RequestBody ValidateBuildRequest request) {
        return ApiResponseDTO.success(pcBuildService.validate(request));
    }
}
