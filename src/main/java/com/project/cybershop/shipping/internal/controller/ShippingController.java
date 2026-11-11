package com.project.cybershop.shipping.internal.controller;

import com.project.cybershop.shared.config.JwtUtil;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import com.project.cybershop.shipping.api.dto.ShippingDtos.*;
import com.project.cybershop.shipping.internal.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "API Vận chuyển GHN (Giao Hàng Nhanh)")
public class ShippingController {

    private final ShippingService shippingService;
    private final JwtUtil jwtUtil;

    @GetMapping("/provinces")
    @Operation(summary = "Lấy danh mục Tỉnh / Thành phố")
    public ResponseEntity<ApiResponseDTO<List<ProvinceDto>>> getProvinces() {
        return ResponseEntity.ok(ApiResponseDTO.success("Thành công", shippingService.getProvinces()));
    }

    @GetMapping("/districts")
    @Operation(summary = "Lấy danh mục Quận / Huyện theo Tỉnh")
    public ResponseEntity<ApiResponseDTO<List<DistrictDto>>> getDistricts(@RequestParam("provinceId") int provinceId) {
        return ResponseEntity.ok(ApiResponseDTO.success("Thành công", shippingService.getDistricts(provinceId)));
    }

    @GetMapping("/wards")
    @Operation(summary = "Lấy danh mục Phường / Xã theo Huyện")
    public ResponseEntity<ApiResponseDTO<List<WardDto>>> getWards(@RequestParam("districtId") int districtId) {
        return ResponseEntity.ok(ApiResponseDTO.success("Thành công", shippingService.getWards(districtId)));
    }

    @PostMapping("/calculate-fee")
    @Operation(summary = "Tính cước vận chuyển GHN theo địa chỉ nhận")
    public ResponseEntity<ApiResponseDTO<CalculateFeeResponse>> calculateFee(@RequestBody CalculateFeeRequest request) {
        return ResponseEntity.ok(ApiResponseDTO.success("Tính cước thành công", shippingService.calculateFee(request)));
    }

    @PatchMapping("/orders/{code}/address")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Staff/Admin: Cập nhật địa chỉ nhận hàng của đơn trước khi tạo vận đơn GHN")
    public ResponseEntity<ApiResponseDTO<String>> updateAddress(
            @PathVariable("code") String orderCode,
            @RequestBody UpdateShippingAddressRequest request
    ) {
        shippingService.updateShippingAddress(orderCode, request);
        return ResponseEntity.ok(ApiResponseDTO.success("Cập nhật địa chỉ đơn hàng thành công", "OK"));
    }

    @PostMapping("/orders/{code}/create-shipment")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Staff/Admin: Đẩy đơn hàng sang GHN để sinh mã vận đơn")
    public ResponseEntity<ApiResponseDTO<TrackingResponse>> createShipment(
            @PathVariable("code") String orderCode,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String email = extractEmail(authHeader);
        TrackingResponse response = shippingService.createShipment(orderCode, email);
        return ResponseEntity.ok(ApiResponseDTO.success("Đẩy đơn sang GHN thành công", response));
    }

    @GetMapping("/orders/{code}/tracking")
    @Operation(summary = "Khách hàng & Staff: Xem Timeline hành trình vận chuyển chi tiết trên web")
    public ResponseEntity<ApiResponseDTO<TrackingResponse>> getTracking(
            @PathVariable("code") String orderCode,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String email = extractEmail(authHeader);
        TrackingResponse response = shippingService.getTracking(orderCode, email);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy thông tin hành trình thành công", response));
    }

    @PostMapping("/orders/{code}/switch-status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Staff/Admin: Mô phỏng hoặc đổi trạng thái đơn GHN (delivering, delivered, cancel)")
    public ResponseEntity<ApiResponseDTO<TrackingResponse>> switchStatus(
            @PathVariable("code") String orderCode,
            @RequestBody SwitchStatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String email = extractEmail(authHeader);
        TrackingResponse response = shippingService.switchStatus(orderCode, request.status(), email);
        return ResponseEntity.ok(ApiResponseDTO.success("Đổi trạng thái thành công", response));
    }

    @GetMapping("/orders/{code}/print-token")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Staff/Admin: Lấy token in phiếu vận đơn A5 của GHN")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> getPrintToken(
            @PathVariable("code") String orderCode
    ) {
        String token = shippingService.getPrintToken(orderCode);
        String printUrl = "https://dev-online-gateway.ghn.vn/a5/index.html?token=" + token;
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy token in tem thành công", Map.of(
                "token", token,
                "printUrl", printUrl
        )));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook nhận thông báo cập nhật trạng thái từ GHN")
    public ResponseEntity<ApiResponseDTO<String>> webhook(@RequestBody Map<String, Object> payload) {
        shippingService.handleWebhook(payload);
        return ResponseEntity.ok(ApiResponseDTO.success("Webhook received", "OK"));
    }

    private String extractEmail(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.getEmailFromToken(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        return null;
    }
}
