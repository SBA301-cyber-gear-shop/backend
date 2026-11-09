package com.project.cybershop.support.internal.controller;

import com.project.cybershop.shared.dto.ApiResponseDTO;
import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.api.dto.response.SupportMessageDto;
import com.project.cybershop.support.api.dto.response.SupportPollResponse;
import com.project.cybershop.support.api.dto.response.SupportSessionDto;
import com.project.cybershop.support.internal.service.CatalogVectorSyncService;
import com.project.cybershop.support.internal.service.CustomerAiSupportService;
import com.project.cybershop.support.internal.service.StaffLiveSupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/customer-support")
@RequiredArgsConstructor
@Tag(name = "Customer Support AI", description = "Trợ lý ảo hỗ trợ khách hàng CyberBot tích hợp RAG & Tra cứu đơn hàng/bảo hành")
public class CustomerAiSupportController {

    private final CustomerAiSupportService supportService;
    private final StaffLiveSupportService liveSupportService;
    private final CatalogVectorSyncService catalogVectorSyncService;

    @PostMapping
    @Operation(summary = "Hỏi đáp với Trợ lý CyberBot (Hỗ trợ cả khách vãng lai và khách đã đăng nhập)")
    public ResponseEntity<ApiResponseDTO<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication
    ) {
        String email = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        AiChatResponse response = supportService.generateResponse(email, request);
        return ResponseEntity.ok(ApiResponseDTO.success("Phản hồi từ CyberBot thành công", response));
    }

    @GetMapping("/messages")
    @Operation(summary = "Khách hàng kiểm tra tin nhắn mới từ chuyên viên (Polling)")
    public ResponseEntity<ApiResponseDTO<SupportPollResponse>> pollMessages(
            @RequestParam String conversationId,
            @RequestParam(required = false) Long afterId
    ) {
        SupportSessionDto session = liveSupportService.getSession(conversationId);
        List<SupportMessageDto> messages = liveSupportService.getMessages(conversationId, afterId);

        SupportPollResponse pollRes = SupportPollResponse.builder()
                .status(session != null ? session.getStatus() : "WAITING")
                .assignedStaffName(session != null ? session.getAssignedStaffName() : null)
                .messages(messages)
                .build();

        return ResponseEntity.ok(ApiResponseDTO.success("Lấy tin nhắn mới thành công", pollRes));
    }

    @PostMapping("/sync-catalog")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Đồng bộ thủ công kho hàng thực tế vào Vector DB CyberBot (Chỉ dành cho Staff/Admin)")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> syncCatalog() {
        Map<String, Object> result = catalogVectorSyncService.syncCatalogVectors();
        return ResponseEntity.ok(ApiResponseDTO.success("Tiến trình đồng bộ kho hàng vào Vector DB hoàn tất", result));
    }

    @GetMapping("/catalog-vector-count")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Kiểm tra số lượng linh kiện trong Vector DB (Chỉ dành cho Staff/Admin)")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getVectorCount() {
        int count = catalogVectorSyncService.getCatalogVectorCount();
        return ResponseEntity.ok(ApiResponseDTO.success("Số lượng vector linh kiện", Map.of("catalogVectorCount", count)));
    }
}
