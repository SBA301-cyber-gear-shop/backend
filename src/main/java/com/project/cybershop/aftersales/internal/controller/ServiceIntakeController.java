package com.project.cybershop.aftersales.internal.controller;

import com.project.cybershop.aftersales.api.dto.request.IntakeCreateRequest;
import com.project.cybershop.aftersales.api.dto.request.NoteRequest;
import com.project.cybershop.aftersales.api.dto.request.NoFaultComponentsRequest;
import com.project.cybershop.aftersales.api.dto.request.RelinkCardRequest;
import com.project.cybershop.aftersales.api.dto.response.ServiceIntakeDto;
import com.project.cybershop.aftersales.internal.service.Actor;
import com.project.cybershop.aftersales.internal.service.CurrentUser;
import com.project.cybershop.aftersales.internal.service.CurrentUser.Principal;
import com.project.cybershop.aftersales.internal.service.ServiceIntakeService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-intakes")
@Tag(name = "Service Intakes", description = "API lần tiếp nhận bảo hành: cả bộ máy khách mang tới, mỗi linh kiện một phiếu con")
public class ServiceIntakeController {

    private final ServiceIntakeService service;
    private final CurrentUser currentUser;

    public ServiceIntakeController(ServiceIntakeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Tiếp nhận một hoặc nhiều linh kiện cùng đơn hàng, mỗi linh kiện một phiếu con")
    public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> create(@Valid @RequestBody IntakeCreateRequest request) {
        Principal actor = currentUser.require(Actor.STAFF, Actor.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Đã tạo phiếu tiếp nhận", service.create(request, actor)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết lần tiếp nhận: các linh kiện, trạng thái chung và thao tác được phép")
    public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> get(@PathVariable("id") Integer id) {
        Principal actor = currentUser.require(Actor.STAFF, Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy lần tiếp nhận thành công", service.get(id, actor)));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Trả cả bộ máy cho khách khi mọi linh kiện đã xử lý xong")
    public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> returnAll(@PathVariable("id") Integer id,
                                                                     @Valid @RequestBody NoteRequest request) {
        Principal actor = currentUser.require(Actor.STAFF, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã trả máy cho khách", service.returnAll(id, request, actor)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy cả lần tiếp nhận khi chưa linh kiện nào được kiểm tra")
    public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> cancel(@PathVariable("id") Integer id,
                                                                  @Valid @RequestBody NoteRequest request) {
        Principal actor = currentUser.require(Actor.STAFF, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã hủy lần tiếp nhận", service.cancel(id, request, actor)));
    }

    @PostMapping("/{id}/requests")
    @Operation(summary = "Thêm một linh kiện cùng đơn vào lần tiếp nhận khi kiểm tra thấy linh kiện đó cũng lỗi")
    public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> addComponent(@PathVariable("id") Integer id,
                                                                        @Valid @RequestBody RelinkCardRequest request) {
        Principal actor = currentUser.require(Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã thêm linh kiện vào lần tiếp nhận",
                service.addComponent(id, request, actor)));
    }

    @PostMapping("/{id}/no-fault-rest")
    @Operation(summary = "Kết luận không tìm thấy lỗi cho các linh kiện được chọn")
    public ResponseEntity<ApiResponseDTO<ServiceIntakeDto>> noFaultRest(@PathVariable("id") Integer id,
                                                                       @Valid @RequestBody NoFaultComponentsRequest request) {
        Principal actor = currentUser.require(Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã kết luận các linh kiện được chọn",
                service.noFaultRest(id, request, actor)));
    }
}
