package com.project.cybershop.aftersales.internal.controller;

import com.project.cybershop.aftersales.api.dto.NoteRequest;
import com.project.cybershop.aftersales.api.dto.RelinkCardRequest;
import com.project.cybershop.aftersales.api.dto.ReplacementRequest;
import com.project.cybershop.aftersales.api.dto.ServiceRequestCreateRequest;
import com.project.cybershop.aftersales.api.dto.ServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.ServiceRequestSummaryDto;
import com.project.cybershop.aftersales.api.dto.TransitionRequest;
import com.project.cybershop.aftersales.internal.service.Actor;
import com.project.cybershop.aftersales.internal.service.CurrentUser;
import com.project.cybershop.aftersales.internal.service.CurrentUser.Principal;
import com.project.cybershop.aftersales.internal.service.ServiceRequestService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
@Tag(name = "Service Requests", description = "API phiếu bảo hành: tiếp nhận, xử lý kỹ thuật, trả máy")
public class ServiceRequestController {

    private final ServiceRequestService service;
    private final CurrentUser currentUser;

    public ServiceRequestController(ServiceRequestService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu tiếp nhận cho một thẻ bảo hành còn hiệu lực")
    public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> create(@Valid @RequestBody ServiceRequestCreateRequest request) {
        Principal actor = currentUser.require(Actor.STAFF, Actor.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Đã tạo phiếu tiếp nhận", service.create(request, actor)));
    }

    @GetMapping
    @Operation(summary = "Danh sách phiếu, lọc theo trạng thái nếu có")
    public ResponseEntity<ApiResponseDTO<List<ServiceRequestSummaryDto>>> list(
            @RequestParam(value = "status", required = false) String status) {
        currentUser.require(Actor.STAFF, Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy danh sách phiếu thành công", service.list(status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu, lịch sử và các bước người gọi được làm tiếp")
    public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> get(@PathVariable("id") Integer id) {
        Principal actor = currentUser.require(Actor.STAFF, Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy phiếu thành công", service.get(id, actor)));
    }

    @PostMapping("/{id}/transitions")
    @Operation(summary = "Chuyển trạng thái phiếu theo bảng chuyển trạng thái của từng vai trò")
    public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> transition(@PathVariable("id") Integer id,
                                                                       @Valid @RequestBody TransitionRequest request) {
        // Vai trò được phép phụ thuộc từng bước, TransitionPolicy kiểm tra trong service.
        Principal actor = currentUser.require(Actor.STAFF, Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã cập nhật phiếu", service.transition(id, request, actor)));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Thêm ghi chú mà không đổi trạng thái")
    public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> addNote(@PathVariable("id") Integer id,
                                                                    @Valid @RequestBody NoteRequest request) {
        Principal actor = currentUser.require(Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã thêm ghi chú", service.addNote(id, request, actor)));
    }

    @PutMapping("/{id}/warranty-card")
    @Operation(summary = "Đổi thẻ gắn với phiếu sang thẻ khác cùng đơn hàng khi đang kiểm tra")
    public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> relinkCard(@PathVariable("id") Integer id,
                                                                       @Valid @RequestBody RelinkCardRequest request) {
        Principal actor = currentUser.require(Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã đổi thẻ bảo hành của phiếu", service.relinkCard(id, request, actor)));
    }

    @PostMapping("/{id}/replacement")
    @Operation(summary = "Đổi thiết bị sang serial khác và chuyển phiếu sang chờ trả")
    public ResponseEntity<ApiResponseDTO<ServiceRequestDto>> replaceDevice(@PathVariable("id") Integer id,
                                                                          @Valid @RequestBody ReplacementRequest request) {
        Principal actor = currentUser.require(Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã ghi nhận đổi thiết bị", service.replaceDevice(id, request, actor)));
    }
}
