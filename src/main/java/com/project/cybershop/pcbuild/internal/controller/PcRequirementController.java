package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;
import com.project.cybershop.pcbuild.internal.service.RequirementService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Controller tiếp nhận yêu cầu phân tích nhu cầu và thẩm định ngân sách (Phase 1).
 * Không chứa business logic, chỉ validate DTO và gọi RequirementService.
 */
@Tag(name = "AI PC Builder", description = "Các API tư vấn và xây dựng cấu hình PC thông minh")
@RestController
@RequiredArgsConstructor
public class PcRequirementController {

    private final RequirementService requirementService;

    @Operation(summary = "Phân tích nhu cầu tự nhiên và kiểm tra ngân sách cấu hình PC")
    @PostMapping({"/api/v1/pc-build/requirements/analyze", "/api/pc-build/requirements/analyze"})
    public ApiResponseDTO<RequirementAnalyzeResponse> analyze(
            @Valid @RequestBody RequirementAnalyzeRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {

        String clientKey = (principal != null && principal.getName() != null && !principal.getName().isBlank())
                ? "user:" + principal.getName()
                : "ip:" + extractClientIp(httpRequest);

        RequirementAnalyzeResponse response = requirementService.analyze(request, clientKey);
        return ApiResponseDTO.success(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}

