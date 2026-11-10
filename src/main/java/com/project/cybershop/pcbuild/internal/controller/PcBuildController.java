package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementRequest;
import com.project.cybershop.pcbuild.api.dto.request.SaveBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.ReplacementOptionResponse;
import com.project.cybershop.pcbuild.api.dto.response.SaveBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedBuildResponse;
import com.project.cybershop.pcbuild.internal.service.PcBuildService;
import com.project.cybershop.pcbuild.internal.support.ClientKeys;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Bước 4–9 của AI Build PC: tự build (kèm lời giải thích AI ở bước 8), kiểm lại, đổi món và lưu bộ PC.
 */
@Tag(name = "AI PC Builder – bước 4–9", description = "Chia ngân sách, chọn linh kiện, kiểm tra, giải thích, đổi món, lưu")
@RestController
@RequestMapping("/api/pc-build/builds")
@RequiredArgsConstructor
public class PcBuildController {

    private final PcBuildService pcBuildService;
    private final IdentityApi identityApi;

    @Operation(summary = "Tự build bộ PC theo ngân sách và mục đích, kèm lời giải thích",
            description = """
                    Lấy `budget` và `purpose` từ `data.requirement` của /api/pc-build/requirements/analyze
                    (trạng thái READY), hoặc purpose khách chọn trong `suggestedPurposes`.
                    Trả bộ PC VALID theo step_order và `message` (bước 8: AI viết, sai số hoặc AI lỗi thì là câu mẫu).
                    Lỗi: 400 PURPOSE_INVALID / BUDGET_TOO_LOW / BUDGET_TOO_HIGH, 422 BUILD_NOT_FOUND, 503 METADATA_INVALID.""")
    @PostMapping("/auto")
    public ApiResponseDTO<BuildResultResponse> autoBuild(@Valid @RequestBody AutoBuildRequest request,
                                                         Principal principal, HttpServletRequest httpRequest) {
        return ApiResponseDTO.success(pcBuildService.autoBuild(request, ClientKeys.of(principal, httpRequest)));
    }

    @Operation(summary = "Kiểm tra lại bộ PC sau khi khách đổi món",
            description = """
                    Nhận ngân sách và danh sách {variantId, qty}. Trả `status` VALID / INCOMPATIBLE (xung đột phần cứng,
                    xem `violations`) / OVER_BUDGET (chỉ vượt tiền, xem `budgetWarning`), kèm `maxQty` thật của từng món.
                    Không gọi AI: `message` luôn null, frontend giữ lời giải thích của lần tự build.
                    Lỗi: 400 VARIANT_NOT_FOUND khi id không tồn tại.""")
    @PostMapping("/validate")
    public ApiResponseDTO<BuildResultResponse> validate(@Valid @RequestBody ValidateBuildRequest request) {
        return ApiResponseDTO.success(pcBuildService.validate(request));
    }

    @Operation(summary = "Danh sách món tương thích cho dropdown đổi món",
            description = """
                    Nhận `category` (mã slot, ví dụ GPU) và bộ PC hiện tại. Chỉ trả món còn hàng, không tạo xung đột mới với
                    các món còn lại, kèm `qty` đề xuất và `maxQty` thật. Lỗi: 400 CATEGORY_INVALID, 400 VARIANT_NOT_FOUND.""")
    @PostMapping("/replacements")
    public ApiResponseDTO<List<ReplacementOptionResponse>> replacements(@Valid @RequestBody ReplacementRequest request) {
        return ApiResponseDTO.success(pcBuildService.replacements(request));
    }

    @Operation(summary = "Lưu bộ PC (cần đăng nhập)",
            description = """
                    Kiểm lại bộ PC với giá mới nhất rồi lưu pc_customs + pc_custom_items. INCOMPATIBLE → 422 BUILD_INCOMPATIBLE.
                    OVER_BUDGET mà chưa có `confirmOverBudget = true` → 409 OVER_BUDGET_NOT_CONFIRMED, `message` là câu popup
                    của bước 7; khách bấm "Tiếp tục" thì gửi lại kèm `confirmOverBudget = true`.""")
    @PostMapping("/save")
    public ApiResponseDTO<SaveBuildResponse> save(@Valid @RequestBody SaveBuildRequest request, Principal principal) {
        Integer userId = requireUserId(principal, "Bạn cần đăng nhập để lưu bộ PC.");
        return ApiResponseDTO.success(pcBuildService.save(request, userId));
    }

    @Operation(summary = "Danh sách bộ PC đã lưu của tài khoản đang đăng nhập",
            description = """
                    Trả các bộ khách tự lưu (pc_customs có order_id rỗng), mới nhất trước, kèm món và giá lúc lưu.
                    Bộ sinh ra khi đặt hàng không nằm ở đây. Chưa có bộ nào thì trả mảng rỗng. Chưa đăng nhập → 401.""")
    @GetMapping("/saved")
    public ApiResponseDTO<List<SavedBuildResponse>> saved(Principal principal) {
        Integer userId = requireUserId(principal, "Bạn cần đăng nhập để xem cấu hình đã lưu.");
        return ApiResponseDTO.success(pcBuildService.listSaved(userId));
    }

    /** SecurityConfig không có matcher cho /api/pc-build/builds/** nên chỗ này là lớp chặn duy nhất: thiếu token → 401. */
    private Integer requireUserId(Principal principal, String message) {
        if (principal == null) {
            throw new InsufficientAuthenticationException(message);
        }
        return identityApi.requireActiveUser(principal.getName()).getId();
    }
}
