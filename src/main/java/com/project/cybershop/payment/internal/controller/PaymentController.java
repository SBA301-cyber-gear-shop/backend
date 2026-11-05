package com.project.cybershop.payment.internal.controller;

import com.project.cybershop.payment.internal.service.PaymentService;
import com.project.cybershop.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final JdbcTemplate jdbc;

    public PaymentController(PaymentService paymentService, JdbcTemplate jdbc) {
        this.paymentService = paymentService;
        this.jdbc = jdbc;
    }

    /**
     * Endpoint nhận kết quả trả về từ Frontend sau khi khách hoàn tất thanh toán VNPay
     */
    @PostMapping("/vnpay/callback")
    public ResponseEntity<Map<String, Object>> handleVnPayCallback(@RequestBody Map<String, String> params) {
        log.info("Received VNPay callback payload from frontend: {}", params);
        boolean success = paymentService.processVnPayCallback(params);
        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("orderCode", orderCode);
        response.put("responseCode", responseCode);
        response.put("message", success ? "Giao dịch thanh toán VNPAY thành công." : "Giao dịch không thành công hoặc đã bị hủy.");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint nhận thông báo IPN từ cổng thanh toán VNPay
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> handleVnPayIpn(@RequestParam Map<String, String> params) {
        log.info("Received VNPay IPN params: {}", params);
        boolean success = paymentService.processVnPayCallback(params);
        if (success) {
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } else {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found or Payment Failed"));
        }
    }

    private record PendingOrderInfo(Integer id, BigDecimal amount, String status) {}

    /**
     * Endpoint tạo lại URL thanh toán cho đơn hàng đang ở trạng thái PENDING
     */
    @PostMapping("/vnpay/create-url")
    public ResponseEntity<Map<String, String>> regeneratePaymentUrl(
            @RequestParam String orderCode,
            HttpServletRequest request
    ) {
        List<PendingOrderInfo> orders = jdbc.query(
                "SELECT id, final_amount, status FROM orders WHERE order_code = ?",
                (rs, i) -> new PendingOrderInfo(
                        rs.getInt("id"),
                        rs.getBigDecimal("final_amount"),
                        rs.getString("status")
                ),
                orderCode
        );

        if (orders.isEmpty()) {
            throw new BusinessException("Không tìm thấy đơn hàng: " + orderCode);
        }

        PendingOrderInfo order = orders.get(0);
        if ("canceled".equalsIgnoreCase(order.status()) || "completed".equalsIgnoreCase(order.status())) {
            throw new BusinessException("Đơn hàng không ở trạng thái cho phép thanh toán.");
        }

        String clientIp = extractClientIp(request);
        String paymentUrl = paymentService.createVnPayPayment(order.id(), orderCode, order.amount(), clientIp);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl, "orderCode", orderCode));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
