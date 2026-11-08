package com.project.cybershop.support.internal.controller;

import com.project.cybershop.shared.dto.ApiResponseDTO;
import com.project.cybershop.support.api.dto.request.SendStaffMessageRequest;
import com.project.cybershop.support.api.dto.response.SupportMessageDto;
import com.project.cybershop.support.api.dto.response.SupportSessionDto;
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

@RestController
@RequestMapping("/api/staff/live-chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Staff Live Support", description = "Trung tâm Hỗ trợ Trực tuyến dành cho Nhân viên CSKH & Quản trị viên")
public class StaffLiveSupportController {

    private final StaffLiveSupportService liveSupportService;

    @GetMapping("/sessions")
    @Operation(summary = "Lấy danh sách các phiên hỗ trợ khách hàng (WAITING, ACTIVE, CLOSED, ALL)")
    public ResponseEntity<ApiResponseDTO<List<SupportSessionDto>>> getSessions(
            @RequestParam(required = false, defaultValue = "ALL") String status
    ) {
        List<SupportSessionDto> sessions = liveSupportService.getSessions(status);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy danh sách phiên hỗ trợ thành công", sessions));
    }

    @GetMapping("/sessions/{conversationId}")
    @Operation(summary = "Lấy chi tiết một phiên hỗ trợ")
    public ResponseEntity<ApiResponseDTO<SupportSessionDto>> getSession(
            @PathVariable String conversationId
    ) {
        SupportSessionDto session = liveSupportService.getSession(conversationId);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy thông tin phiên hỗ trợ thành công", session));
    }

    @GetMapping("/sessions/{conversationId}/messages")
    @Operation(summary = "Lấy danh sách tin nhắn trong phiên (hỗ trợ polling sau afterId)")
    public ResponseEntity<ApiResponseDTO<List<SupportMessageDto>>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) Long afterId
    ) {
        List<SupportMessageDto> messages = liveSupportService.getMessages(conversationId, afterId);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy tin nhắn thành công", messages));
    }

    @PostMapping("/sessions/{conversationId}/assign")
    @Operation(summary = "Chuyên viên nhận phụ trách phiên hỗ trợ khách hàng")
    public ResponseEntity<ApiResponseDTO<SupportSessionDto>> assignStaff(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        SupportSessionDto session = liveSupportService.assignStaff(conversationId, email);
        return ResponseEntity.ok(ApiResponseDTO.success("Tiếp nhận phiên hỗ trợ thành công", session));
    }

    @PostMapping("/sessions/{conversationId}/messages")
    @Operation(summary = "Chuyên viên gửi tin nhắn phản hồi cho khách hàng")
    public ResponseEntity<ApiResponseDTO<SupportMessageDto>> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendStaffMessageRequest request,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        SupportMessageDto message = liveSupportService.sendStaffMessage(conversationId, email, request.getContent());
        return ResponseEntity.ok(ApiResponseDTO.success("Gửi tin nhắn phản hồi thành công", message));
    }

    @PostMapping("/sessions/{conversationId}/close")
    @Operation(summary = "Chuyên viên kết thúc phiên hỗ trợ")
    public ResponseEntity<ApiResponseDTO<SupportSessionDto>> closeSession(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        SupportSessionDto session = liveSupportService.closeSession(conversationId, email);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã kết thúc phiên hỗ trợ", session));
    }
}
