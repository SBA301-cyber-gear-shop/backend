package com.project.cybershop.payment.internal.service.impl;

import com.project.cybershop.payment.api.PaymentApi;
import com.project.cybershop.payment.internal.service.PaymentService;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public PaymentServiceImpl(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock=clock; }

    @Override
    public void createCounterPayment(Integer orderId, BigDecimal amount) {
        boolean free = amount.signum() == 0;
        jdbc.update("""
                INSERT INTO payments (order_id,method_type,display_name,amount,status,created_at,paid_at)
                VALUES (?,'cash','Thanh toán tại cửa hàng',?,?,?,?)
                """, orderId, amount, free ? "SUCCESS" : "PENDING", LocalDateTime.now(clock), free ? LocalDateTime.now(clock) : null);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment get(Integer orderId) {
        var payments = jdbc.query("SELECT status,method_type,amount,paid_at FROM payments WHERE order_id=?",
                (rs, i) -> new Payment(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getObject(4, LocalDateTime.class)), orderId);
        if (payments.isEmpty()) { return new Payment("PENDING", "cash", BigDecimal.ZERO, null); }
        return payments.get(0);
    }

    @Override
    public void collect(Integer orderId, BigDecimal receivedAmount, String reference) {
        lockStoredPayment(orderId);
        var payment = get(orderId);
        if (receivedAmount == null || receivedAmount.compareTo(payment.amount()) < 0)
            throw new BusinessException("Chưa nhận đủ tiền. Chưa thể xác nhận thanh toán.");
        if ("SUCCESS".equals(payment.status())) return;
        if (!"cash".equals(payment.methodType()) || !"PENDING".equals(payment.status()))
            throw new BusinessException("Khoản thanh toán không thể được thu tại quầy.");
        jdbc.update("UPDATE payments SET status='SUCCESS',paid_at=?,transaction_reference=? WHERE order_id=?",
                LocalDateTime.now(clock), reference == null || reference.isBlank() ? null : reference.trim(), orderId);
    }

    @Override
    public void cancel(Integer orderId) {
        lockStoredPayment(orderId);
        var payment = get(orderId);
        if ("SUCCESS".equals(payment.status()) && payment.amount().signum() > 0)
            throw new BusinessException("Đơn đã thanh toán cần được xử lý hoàn tiền trước khi hủy.");
        jdbc.update("UPDATE payments SET status='CANCELED' WHERE order_id=?", orderId);
    }

    private void lockStoredPayment(Integer orderId) {
        if (jdbc.queryForList("SELECT id FROM payments WHERE order_id=? FOR UPDATE", orderId).size() != 1) {
            throw new BusinessException("Không tìm thấy khoản thanh toán hợp lệ của đơn hàng.");
        }
    }
}
