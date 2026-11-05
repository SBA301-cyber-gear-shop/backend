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
    public String createVnPayPayment(Integer orderId, String orderCode, BigDecimal amount, String clientIp) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) {
            jdbc.update("""
                    UPDATE payments SET method_type='vnpay', provider='VNPAY',
                    display_name='VNPAY Sandbox (QR / ATM / Internet Banking)',
                    amount=?, status='PENDING', paid_at=NULL WHERE order_id=?
                    """, amount, orderId);
        } else {
            jdbc.update("""
                    INSERT INTO payments (order_id, method_type, provider, display_name, amount, status, created_at, paid_at)
                    VALUES (?, 'vnpay', 'VNPAY', 'VNPAY Sandbox (QR / ATM / Internet Banking)', ?, 'PENDING', ?, NULL)
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

        List<Integer> orderIds = jdbc.query(
                "SELECT id FROM orders WHERE order_code = ?",
                (rs, i) -> rs.getInt(1),
                orderCode
        );

        if (orderIds.isEmpty()) {
            log.warn("VNPay callback order not found for orderCode: {}", orderCode);
            return false;
        }

        Integer orderId = orderIds.get(0);
        boolean isSuccess = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (isSuccess) {
            jdbc.update(
                    "UPDATE payments SET status='SUCCESS', paid_at=?, transaction_reference=? WHERE order_id=?",
                    LocalDateTime.now(clock), transactionNo, orderId
            );
            // Kích hoạt voucher nếu có
            jdbc.update("UPDATE promotion_usages SET status='used' WHERE order_id=?", orderId);
            log.info("VNPay payment SUCCESS for orderId: {}, code: {}", orderId, orderCode);
            return true;
        } else {
            jdbc.update(
                    "UPDATE payments SET status='FAILED', transaction_reference=? WHERE order_id=?",
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
        jdbc.queryForList("SELECT id FROM payments WHERE order_id = ? FOR UPDATE", orderId);
        var payment = get(orderId);
        if (receivedAmount == null || receivedAmount.compareTo(payment.amount()) < 0) {
            throw new BusinessException("Chưa nhận đủ tiền. Chưa thể xác nhận thanh toán.");
        }
        if ("SUCCESS".equals(payment.status())) {
            return;
        }
        if (!"cash".equals(payment.methodType()) || !"PENDING".equals(payment.status())) {
            throw new BusinessException("Khoản thanh toán không thể được thu tại quầy.");
        }
        jdbc.update(
                "UPDATE payments SET status='SUCCESS', paid_at=?, transaction_reference=? WHERE order_id=?",
                LocalDateTime.now(clock), reference == null || reference.isBlank() ? null : reference.trim(), orderId
        );
    }

    @Override
    public void cancel(Integer orderId) {
        var payment = get(orderId);
        if ("SUCCESS".equals(payment.status()) && payment.amount().signum() > 0) {
            throw new BusinessException("Đơn đã thanh toán cần được xử lý hoàn tiền trước khi hủy.");
        }
        jdbc.update("UPDATE payments SET status='CANCELED' WHERE order_id = ?", orderId);
    }
}
