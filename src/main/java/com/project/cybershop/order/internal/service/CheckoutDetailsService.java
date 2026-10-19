package com.project.cybershop.order.internal.service;


import com.project.cybershop.order.api.dto.CheckoutDetailsRequest;
import com.project.cybershop.order.api.dto.CheckoutDetailsResponse;
import com.project.cybershop.order.api.dto.CheckoutPreviewResponse;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class CheckoutDetailsService {
    private final CheckoutService checkoutService;
    private final JdbcTemplate jdbc;

    public CheckoutDetailsService(CheckoutService checkoutService, JdbcTemplate jdbc) {
        this.checkoutService = checkoutService;
        this.jdbc = jdbc;
    }

    // Chỉ tính thử, không tạo Orders, Promotion_Usages hoặc giữ voucher.
    @Transactional(readOnly = true)
    public CheckoutDetailsResponse preview(String loginEmail, CheckoutDetailsRequest request) {
        if (request == null) {
            throw new BusinessException("Thiếu thông tin nhận hàng.");
        }
        String pickupName = trim(request.pickupName());
        String pickupPhone = trim(request.pickupPhone());
        String note = trim(request.note());
        String code = trim(request.promotionCode());

        if (pickupName.isEmpty() || pickupName.length() > 100) {
            throw new BusinessException("Tên người nhận phải từ 1 đến 100 ký tự.");
        }
        if (!pickupPhone.matches("^[0-9+ .()-]{9,20}$")) {
            throw new BusinessException("Số điện thoại nhận hàng không hợp lệ.");
        }
        if (code.length() > 50) {
            throw new BusinessException("Mã voucher quá dài.");
        }

        // Tận dụng kiểm tra giỏ hàng của bước 4: sản phẩm, tồn kho, giá hiện tại.
        CheckoutPreviewResponse cart = checkoutService.validate(loginEmail);
        BigDecimal total = cart.totalAmount();
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCode = null;

        if (!code.isEmpty()) {
            Integer userId = findUserId(loginEmail);
            Promotion promotion = findPromotion(code);
            if (!promotion.active() || !promotion.withinPeriod()) {
                throw new BusinessException("Voucher không hoạt động hoặc ngoài thời hạn sử dụng.");
            }
            if (promotion.usageLimit() <= 0) {
                throw new BusinessException("Voucher đã hết lượt sử dụng.");
            }
            Integer usedByCustomer = jdbc.queryForObject("""
                    SELECT count(*) FROM "Promotion_Usages"
                    WHERE "promotion_id" = ? AND "user_id" = ?
                    """, Integer.class, promotion.id(), userId);
            if (usedByCustomer != null && usedByCustomer > 0) {
                throw new BusinessException("Bạn đã sử dụng voucher này.");
            }
            // Bảng có status enum tạm thời; tính mọi lượt ghi nhận (bao gồm RESERVED),
            // tránh cấp vượt số lượt trong giai đoạn preview.
            Integer usageCount = jdbc.queryForObject("""
                    SELECT count(*) FROM "Promotion_Usages" WHERE "promotion_id" = ?
                    """, Integer.class, promotion.id());
            if (usageCount != null && usageCount >= promotion.usageLimit()) {
                throw new BusinessException("Voucher đã hết lượt sử dụng.");
            }
            discount = calculateDiscount(promotion, total);
            appliedCode = promotion.code();
        }

        BigDecimal finalAmount = total.subtract(discount).max(BigDecimal.ZERO);
        return new CheckoutDetailsResponse(cart.cartId(), pickupName, pickupPhone,
                note, appliedCode, cart.items(), total, discount, finalAmount);
    }

    private Integer findUserId(String email) {
        List<Integer> ids = jdbc.query("""
                SELECT "id" FROM "Users"
                WHERE lower("email") = lower(?) AND "is_active" = true
                """, (rs, i) -> rs.getInt(1), email);
        if (ids.isEmpty()) throw new BusinessException("Không tìm thấy người dùng.");
        return ids.get(0);
    }

    private Promotion findPromotion(String code) {
        // Không assume code unique vì schema hiện tại chưa có UNIQUE trên Promotions.code.
        List<Promotion> matches = jdbc.query("""
                SELECT "id", "code", "discount_type", "discount_value",
                       "usage_limit", "is_active",
                       (now() BETWEEN "start_date" AND "end_date") AS valid_period
                FROM "Promotions"
                WHERE lower("code") = lower(?)
                LIMIT 2
                """, (rs, i) -> new Promotion(
                    rs.getInt("id"), rs.getString("code"),
                    rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
                    rs.getInt("usage_limit"), Boolean.TRUE.equals(rs.getObject("is_active")),
                    rs.getBoolean("valid_period")), code);
        if (matches.isEmpty()) throw new BusinessException("Mã voucher không tồn tại.");
        if (matches.size() > 1) throw new BusinessException("Mã voucher bị trùng trong hệ thống.");
        return matches.get(0);
    }

    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal total) {
        BigDecimal value = promotion.discountValue();
        if (value == null || value.signum() < 0) {
            throw new BusinessException("Giá trị voucher không hợp lệ.");
        }
        String type = promotion.discountType() == null ? "" :
                promotion.discountType().trim().toLowerCase(Locale.ROOT);
        BigDecimal discount;
        switch (type) {
            case "percent", "percentage" -> {
                if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessException("Phần trăm giảm giá không hợp lệ.");
                }
                discount = total.multiply(value)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case "fixed", "fixed_amount" -> discount = value;
            default -> throw new BusinessException(
                    "Loại giảm giá chưa hỗ trợ: " + promotion.discountType());
        }
        return discount.min(total);
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }

    private record Promotion(Integer id, String code, String discountType,
                             BigDecimal discountValue, Integer usageLimit,
                             boolean active, boolean withinPeriod) {}
}
