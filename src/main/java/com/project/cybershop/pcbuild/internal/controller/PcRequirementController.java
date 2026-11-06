package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementOptionsResponse;
import com.project.cybershop.pcbuild.internal.service.RequirementService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/pc-build/requirements")
@RequiredArgsConstructor
@Tag(name = "PC Build – Phần A: hiểu yêu cầu", description = "Bước 1–3 của AI Build PC: đọc yêu cầu và kiểm tra ngân sách")
public class PcRequirementController {

    private final RequirementService requirementService;

    @GetMapping("/options")
    @Operation(summary = "Danh sách giá trị hợp lệ",
            description = "Mục đích (kèm ngân sách tối thiểu), mức nhu cầu, cỡ main, hãng CPU còn hàng và cận trên ngân sách. "
                    + "Dùng cho form khi trạng thái là FORM_REQUIRED và cho popup đổi mục đích.")
    public ApiResponseDTO<RequirementOptionsResponse> options() {
        return ApiResponseDTO.success(requirementService.options());
    }

    @PostMapping("/analyze")
    @Operation(summary = "Đọc và kiểm tra yêu cầu",
            description = """
                    - Có `message` và không có `purpose`: gọi AI đọc câu của khách (tính vào giới hạn tần suất).
                    - Có `purpose` (khách đồng ý popup hoặc gửi form): không gọi AI, kiểm tra nguyên giá trị gửi lên; sai danh sách → 400.
                    - `budget` ở ô riêng luôn thắng số tiền AI đọc từ câu.
                    - Trạng thái: READY (đi tiếp bước 4), NEED_MORE_INFO (hỏi lại, gửi lại `clarifyCount` trong response),
                      FORM_REQUIRED (hỏi quá 2 lần hoặc AI lỗi), BELOW_MIN_BUDGET (popup gợi ý `suggestedPurposes`),
                      NO_AFFORDABLE_PURPOSE, BUDGET_TOO_HIGH.""")
    public ApiResponseDTO<RequirementAnalyzeResponse> analyze(@Valid @RequestBody RequirementAnalyzeRequest request,
                                                              Principal principal, HttpServletRequest httpRequest) {
        String clientKey = principal != null ? "user:" + principal.getName() : "ip:" + httpRequest.getRemoteAddr();
        return ApiResponseDTO.success(requirementService.analyze(request, clientKey));
    }
}
