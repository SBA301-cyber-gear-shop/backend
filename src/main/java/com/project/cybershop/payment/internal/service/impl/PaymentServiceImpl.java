package com.project.cybershop.payment.internal.service.impl;

import com.project.cybershop.payment.api.PaymentApi;
import com.project.cybershop.payment.internal.config.VnPayConfig;
import com.project.cybershop.payment.internal.service.PaymentService;
import com.project.cybershop.payment.internal.util.VnPayUtil;
import com.project.cybershop.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final DateTimeFormatter VNP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final VnPayConfig vnPayConfig;

    public PaymentServiceImpl(JdbcTemplate jdbc, Clock clock, VnPayConfig vnPayConfig) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.vnPayConfig = vnPayConfig;
    }

    @Override
    public void createCounterPayment(Integer orderId, BigDecimal amount) {
        boolean free = amount.signum() == 0;
        Integer count = jdbc.queryForObject("SELECT count(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) {
            jdbc.update("""
                    UPDATE payments SET method_type='cash', display_name='Thanh toán tại cửa hàng',
                    amount=?, status=?, paid_at=? WHERE order_id=?
                    """, amount, free ? "SUCCESS" : "PENDING", free ? LocalDateTime.now(clock) : null, orderId);
        } else {
            jdbc.update("""
                    INSERT INTO payments (order_id, method_type, display_name, amount, status, created_at, paid_at)
                    VALUES (?, 'cash', 'Thanh toán tại cửa hàng', ?, ?, ?, ?)
                    """, orderId, amount, free ? "SUCCESS" : "PENDING", LocalDateTime.now(clock), free ? LocalDateTime.now(clock) : null);
        }
    }

    @Override
    public void createCodPayment(Integer orderId, BigDecimal amount) {
        boolean free = amount.signum() == 0;
        Integer count = jdbc.queryForObject("SELECT count(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) {
            jdbc.update("""
                    UPDATE payments SET method_type='cod', provider='GHN_COD',
                    display_name='Thanh toán khi nhận hàng (COD)',
                    amount=?, status=?, paid_at=? WHERE order_id=?
                    """, amount, free ? "SUCCESS" : "PENDING", free ? LocalDateTime.now(clock) : null, orderId);
        } else {
            jdbc.update("""
                    INSERT INTO payments (order_id, method_type, provider, display_name, amount, status, created_at, paid_at)
                    VALUES (?, 'cod', 'GHN_COD', 'Thanh toán khi nhận hàng (COD)', ?, ?, ?, ?)
                    """, orderId, amount, free ? "SUCCESS" : "PENDING", LocalDateTime.now(clock), free ? LocalDateTime.now(clock) : null);
        }
    }

    @Override
    public String createVnPayPayment(Integer orderId, String orderCode, BigDecimal amount, String clientIp) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) {
            jdbc.update("""
                    UPDATE payments SET method_type='vnpay', provider='VNPAY',
                    display_name='VNPAY (QR / ATM / Internet Banking)',
                    amount=?, status='PENDING', paid_at=NULL WHERE order_id=?
                    """, amount, orderId);
        } else {
            jdbc.update("""
                    INSERT INTO payments (order_id, method_type, provider, display_name, amount, status, created_at, paid_at)
                    VALUES (?, 'vnpay', 'VNPAY', 'VNPAY (QR / ATM / Internet Banking)', ?, 'PENDING', ?, NULL)
                    """, orderId, amount, LocalDateTime.now(clock));
        }

        return generateVnPayUrl(orderCode, amount, clientIp);
    }

    @Override
    public String generateVnPayUrl(String orderCode, BigDecimal amount, String clientIp) {
        String createDate = LocalDateTime.now(clock).format(VNP_DATE_FORMAT);
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
        String ipAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderCode);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode + " tai CyberShop");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr);
        params.put("vnp_CreateDate", createDate);

        return VnPayUtil.buildPaymentUrl(vnPayConfig.getPayUrl(), params, vnPayConfig.getHashSecret());
    }

    @Override
    public boolean processVnPayCallback(Map<String, String> params) {
        log.info("Processing VNPay callback with params: {}", params);
        boolean validSignature = VnPayUtil.verifySignature(params, vnPayConfig.getHashSecret());
        if (!validSignature) {
            log.error("VNPay callback signature verification failed!");
            return false;
        }

        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");
        String vnpAmountStr = params.get("vnp_Amount");

        List<Map<String, Object>> orderRows = jdbc.queryForList(
                "SELECT o.id, o.final_amount, o.status as order_status, p.status as payment_status " +
                "FROM orders o LEFT JOIN payments p ON o.id = p.order_id WHERE o.order_code = ?",
                orderCode
        );

        if (orderRows.isEmpty()) {
            log.warn("VNPay callback order not found for orderCode: {}", orderCode);
            return false;
        }

        Map<String, Object> row = orderRows.get(0);
        Integer orderId = ((Number) row.get("id")).intValue();
        BigDecimal expectedAmount = (BigDecimal) row.get("final_amount");
        String currentPaymentStatus = (String) row.get("payment_status");

        // 1. Idempotency Guard: Nếu đã thanh toán thành công trước đó (do IPN hoặc ReturnURL đến trước), giữ nguyên kết quả
        if ("SUCCESS".equalsIgnoreCase(currentPaymentStatus)) {
            log.info("Order {} already marked as SUCCESS. Idempotent return true.", orderCode);
            return true;
        }

        // 2. Amount Verification: Kiểm tra khớp số tiền (vnp_Amount nhân 100)
        if (vnpAmountStr != null && !vnpAmountStr.isBlank() && expectedAmount != null) {
            try {
                long returnedVnd = Long.parseLong(vnpAmountStr.trim()) / 100L;
                long expectedVnd = expectedAmount.longValue();
                if (returnedVnd != expectedVnd) {
                    log.error("VNPay amount mismatch for order {}: expected {}, got {}", orderCode, expectedVnd, returnedVnd);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.error("Invalid vnp_Amount format: {}", vnpAmountStr);
                return false;
            }
        }

        boolean isSuccess = "00".equals(responseCode) && (transactionStatus == null || "00".equals(transactionStatus));

        if (isSuccess) {
            LocalDateTime now = LocalDateTime.now(clock);
            jdbc.update(
                    "UPDATE payments SET status='SUCCESS', paid_at=?, transaction_reference=? WHERE order_id=?",
                    now, transactionNo, orderId
            );
            // Tự động chuyển trạng thái đơn hàng từ pending sang confirmed theo đúng State Machine
            jdbc.update(
                    "UPDATE orders SET status='confirmed', updated_at=? WHERE id=? AND status='pending'",
                    now, orderId
            );
            // Kích hoạt voucher nếu có
            jdbc.update("UPDATE promotion_usages SET status='used' WHERE order_id=?", orderId);
            log.info("VNPay payment SUCCESS & order CONFIRMED for orderId: {}, code: {}", orderId, orderCode);
            return true;
        } else {
            jdbc.update(
                    "UPDATE payments SET status='FAILED', transaction_reference=? WHERE order_id=? AND status != 'SUCCESS'",
                    transactionNo, orderId
            );
            log.warn("VNPay payment FAILED or CANCELLED (code: {}) for orderId: {}, code: {}", responseCode, orderId, orderCode);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Payment get(Integer orderId) {
        var payments = jdbc.query(
                "SELECT status, method_type, amount, paid_at FROM payments WHERE order_id = ?",
                (rs, i) -> new Payment(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getObject(4, LocalDateTime.class)),
                orderId
        );
        if (payments.isEmpty()) {
            return new Payment("PENDING", "cash", BigDecimal.ZERO, null);
        }
        return payments.get(0);
    }

    @Override
    public void collect(Integer orderId, BigDecimal receivedAmount, String reference) {
        lockStoredPayment(orderId);
        var payment = get(orderId);
        if (receivedAmount == null || receivedAmount.compareTo(payment.amount()) < 0) {
            throw new BusinessException("Chưa nhận đủ tiền. Chưa thể xác nhận thanh toán.");
        }
        if ("SUCCESS".equals(payment.status())) {
            return;
        }
        if (!Set.of("cash", "cod").contains(payment.methodType().toLowerCase()) || !"PENDING".equals(payment.status())) {
            throw new BusinessException("Khoản thanh toán không ở trạng thái chờ thu tiền mặt/COD.");
        }
        jdbc.update(
                "UPDATE payments SET status='SUCCESS', paid_at=?, transaction_reference=? WHERE order_id=?",
                LocalDateTime.now(clock), reference == null || reference.isBlank() ? null : reference.trim(), orderId
        );
    }

    @Override
    public void cancel(Integer orderId) {
        lockStoredPayment(orderId);
        var payment = get(orderId);
        if ("SUCCESS".equals(payment.status()) && payment.amount().signum() > 0) {
            // Đánh dấu hoàn tiền (REFUNDED) khi hủy đơn đã thanh toán thay vì chặn cứng
            jdbc.update("UPDATE payments SET status='REFUNDED' WHERE order_id = ?", orderId);
            return;
        }
        jdbc.update("UPDATE payments SET status='CANCELED' WHERE order_id = ?", orderId);
    }

    private void lockStoredPayment(Integer orderId) {
        if (jdbc.queryForList("SELECT id FROM payments WHERE order_id=? FOR UPDATE", orderId).size() != 1) {
            throw new BusinessException("Không tìm thấy khoản thanh toán hợp lệ của đơn hàng.");
        }
    }
}
