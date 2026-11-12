package com.project.cybershop.support.internal.repository;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CustomerContext;
import com.project.cybershop.support.internal.model.CustomerContext.OrderSummary;
import com.project.cybershop.support.internal.model.CustomerContext.ServiceRequestSummary;
import com.project.cybershop.support.internal.model.CustomerContext.WarrantySummary;
import com.project.cybershop.support.internal.model.StorePromotionContext;
import com.project.cybershop.support.internal.model.StorePromotionContext.FlashSaleSummary;
import com.project.cybershop.support.internal.model.StorePromotionContext.PromotionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SupportCustomerContextRepository {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final JdbcTemplate jdbcTemplate;

    public CustomerContext fetchCustomerContext(String email) {
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return new CustomerContext(null, null, null, List.of(), List.of(), List.of());
        }

        try {
            List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                    "SELECT id, full_name, phone FROM users WHERE email = ? LIMIT 1", email);
            if (userRows.isEmpty()) {
                return new CustomerContext(null, null, null, List.of(), List.of(), List.of());
            }

            Integer userId = (Integer) userRows.get(0).get("id");
            String fullName = (String) userRows.get(0).get("full_name");
            String phone = (String) userRows.get(0).get("phone");

            String orderSql = """
                SELECT o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at,
                       string_agg(oi.product_name || ' (x' || oi.quantity || ')', ', ') as items
                FROM orders o
                LEFT JOIN order_items oi ON o.id = oi.order_id
                WHERE o.user_id = ?
                GROUP BY o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at
                ORDER BY o.created_at DESC
                LIMIT 5
            """;
            List<OrderSummary> orders = jdbcTemplate.query(orderSql, (rs, rowNum) -> new OrderSummary(
                    rs.getInt("id"),
                    rs.getString("order_code"),
                    rs.getString("status"),
                    rs.getString("delivery_type"),
                    rs.getString("tracking_code"),
                    rs.getDouble("final_amount"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().format(DATE_FMT) : "",
                    rs.getString("items") != null ? rs.getString("items") : "Linh kiện PC"
            ), userId);

            String warrantySql = """
                SELECT wc.id, wc.serial_number, oi.product_name, wc.end_date, wc.status
                FROM warranty_cards wc
                JOIN order_items oi ON wc.order_item_id = oi.id
                JOIN orders o ON oi.order_id = o.id
                WHERE o.user_id = ?
                ORDER BY wc.end_date DESC
                LIMIT 5
            """;
            LocalDateTime now = LocalDateTime.now();
            List<WarrantySummary> warranties = jdbcTemplate.query(warrantySql, (rs, rowNum) -> {
                LocalDateTime end = rs.getTimestamp("end_date") != null ? rs.getTimestamp("end_date").toLocalDateTime() : null;
                long days = (end != null) ? Math.max(0, Duration.between(now, end).toDays()) : 0;
                return new WarrantySummary(
                        rs.getInt("id"),
                        rs.getString("serial_number"),
                        rs.getString("product_name"),
                        end != null ? end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                        days,
                        rs.getString("status")
                );
            }, userId);

            List<ServiceRequestSummary> serviceRequests = List.of();
            try {
                String serviceRequestSql = """
                    SELECT sr.id, sr.request_code, sr.status, sr.issue_description, sr.expected_return_date,
                           wc.serial_number, oi.product_name
                    FROM service_requests sr
                    JOIN warranty_cards wc ON sr.warranty_card_id = wc.id
                    JOIN order_items oi ON wc.order_item_id = oi.id
                    WHERE sr.user_id = ?
                    ORDER BY sr.id DESC
                    LIMIT 5
                """;
                serviceRequests = jdbcTemplate.query(serviceRequestSql, (rs, rowNum) -> {
                    String rawStatus = rs.getString("status");
                    String viStatus = mapServiceStatusVietnamese(rawStatus);
                    java.sql.Date expDate = rs.getDate("expected_return_date");
                    return new ServiceRequestSummary(
                            rs.getInt("id"),
                            rs.getString("request_code"),
                            rs.getString("serial_number"),
                            rs.getString("product_name"),
                            rawStatus,
                            viStatus,
                            rs.getString("issue_description") != null ? rs.getString("issue_description") : "Kiểm tra kỹ thuật",
                            expDate != null ? expDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Đang cập nhật"
                    );
                }, userId);
            } catch (Exception ex) {
                log.debug("Bảng service_requests chưa có hoặc có lỗi tra cứu phiếu bảo hành: {}", ex.getMessage());
            }

            return new CustomerContext(userId, fullName, phone, orders, warranties, serviceRequests);
        } catch (Exception e) {
            log.warn("Lỗi khi nạp dữ liệu khách hàng cho AI: {}", e.getMessage());
            return new CustomerContext(null, null, null, List.of(), List.of(), List.of());
        }
    }

    public StorePromotionContext fetchActivePromotionsAndFlashSales() {
        try {
            String promoSql = """
                SELECT code, name, discount_type, discount_value, end_date
                FROM promotions
                WHERE is_active = true AND (start_date IS NULL OR start_date <= NOW()) AND (end_date IS NULL OR end_date >= NOW())
                ORDER BY id DESC
                LIMIT 5
            """;
            List<PromotionSummary> vouchers = jdbcTemplate.query(promoSql, (rs, rowNum) -> {
                LocalDateTime end = rs.getTimestamp("end_date") != null ? rs.getTimestamp("end_date").toLocalDateTime() : null;
                return new PromotionSummary(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value"),
                        end != null ? end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Không thời hạn"
                );
            });

            String saleSql = """
                SELECT p.id as product_id, pv.id as variant_id, p.name as product_name, pv.variant_name,
                       pv.price as live_price, pv.original_price as original_price, pv.image_url,
                       ps.discount_type, ps.discount_value, ps.percentage
                FROM product_sales ps
                JOIN products p ON ps.product_id = p.id
                JOIN product_variants pv ON pv.product_id = p.id
                WHERE ps.enabled = true AND (ps.start_date IS NULL OR ps.start_date <= NOW()) AND (ps.end_date IS NULL OR ps.end_date >= NOW())
                  AND pv.price < pv.original_price
                ORDER BY (pv.original_price - pv.price) DESC
                LIMIT 5
            """;
            List<FlashSaleSummary> flashSales = jdbcTemplate.query(saleSql, (rs, rowNum) -> {
                double price = rs.getDouble("live_price");
                double orig = rs.getDouble("original_price");
                String dType = rs.getString("discount_type");
                BigDecimal dVal = rs.getBigDecimal("discount_value");
                BigDecimal pct = rs.getBigDecimal("percentage");
                String discountDesc = "Giảm giá sốc";
                if ("percentage".equalsIgnoreCase(dType) && pct != null) {
                    discountDesc = "Giảm " + pct.intValue() + "%";
                } else if (dVal != null) {
                    discountDesc = "Giảm " + MONEY_FMT.format(dVal.doubleValue());
                }
                return new FlashSaleSummary(
                        rs.getInt("product_id"),
                        rs.getInt("variant_id"),
                        rs.getString("product_name"),
                        rs.getString("variant_name"),
                        price,
                        orig,
                        discountDesc,
                        rs.getString("image_url")
                );
            });

            return new StorePromotionContext(vouchers, flashSales);
        } catch (Exception e) {
            log.warn("Lỗi khi nạp chương trình khuyến mãi/flash sale: {}", e.getMessage());
            return new StorePromotionContext(List.of(), List.of());
        }
    }

    public String resolveDynamicOrderContext(String message) {
        if (message == null || message.isBlank()) return null;
        Matcher m = Pattern
                .compile("\\b(TZ-[A-Za-z0-9]+|ORD-[A-Za-z0-9]+|DH[A-Za-z0-9]+)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(message);
        if (m.find()) {
            String code = m.group(1);
            try {
                String sql = """
                    SELECT o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at,
                           string_agg(oi.product_name || ' (x' || oi.quantity || ')', ', ') as items
                    FROM orders o
                    LEFT JOIN order_items oi ON o.id = oi.order_id
                    WHERE UPPER(o.order_code) = UPPER(?)
                    GROUP BY o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at
                    LIMIT 1
                """;
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, code);
                if (!rows.isEmpty()) {
                    Map<String, Object> r = rows.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("* **Mã đơn hàng**: `").append(r.get("order_code")).append("`\n");
                    sb.append("* **Trạng thái**: **").append(mapStatusVietnamese((String) r.get("status"))).append("**\n");
                    if (r.get("tracking_code") != null) {
                        sb.append("* **Mã vận đơn GHN**: `").append(r.get("tracking_code")).append("`\n");
                    }
                    if (r.get("final_amount") != null) {
                        sb.append("* **Tổng thanh toán**: **").append(MONEY_FMT.format(((Number) r.get("final_amount")).doubleValue())).append("**\n");
                    }
                    if (r.get("items") != null) {
                        sb.append("* **Sản phẩm trong đơn**: ").append(r.get("items")).append("\n");
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                log.warn("Lỗi tra cứu đơn theo mã tự do: {}", e.getMessage());
            }
        }
        return null;
    }

    public String buildDynamicPromotionAdvisory(double totalAmount) {
        if (totalAmount <= 0) return "";
        try {
            StorePromotionContext promoCtx = fetchActivePromotionsAndFlashSales();
            if (promoCtx.activeVouchers().isEmpty()) {
                return "";
            }

            PromotionSummary bestVoucher = null;
            double maxSavings = 0;

            for (PromotionSummary p : promoCtx.activeVouchers()) {
                double savings = 0;
                if ("percentage".equalsIgnoreCase(p.discountType()) && p.discountValue() != null) {
                    savings = totalAmount * (p.discountValue().doubleValue() / 100.0);
                } else if (p.discountValue() != null) {
                    savings = p.discountValue().doubleValue();
                }
                if (savings > maxSavings && savings < totalAmount) {
                    maxSavings = savings;
                    bestVoucher = p;
                }
            }

            if (bestVoucher != null && maxSavings > 0) {
                double finalPay = Math.max(0, totalAmount - maxSavings);
                StringBuilder sb = new StringBuilder();
                sb.append("\n🎁 **Mẹo tiết kiệm từ TechZone**:\n");
                sb.append("• Đơn hàng của bạn đạt điều kiện áp dụng mã giảm giá **").append(bestVoucher.code()).append("** (").append(bestVoucher.name()).append(")!\n");
                sb.append("• Nhập mã `").append(bestVoucher.code()).append("` tại bước thanh toán để được **giảm ngay ").append(MONEY_FMT.format(maxSavings)).append("** (tổng thanh toán trọn bộ chỉ còn **").append(MONEY_FMT.format(finalPay)).append("**).\n");
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tính toán khuyến mãi động: {}", e.getMessage());
        }
        return "";
    }

    public AiChatResponse.StaffInfo getAvailableOnlineStaff() {
        try {
            String sql = """
                SELECT u.id, u.full_name, u.email, u.phone, r.name as role_name
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE UPPER(r.name) IN ('STAFF', 'ADMIN')
                ORDER BY u.id ASC
                LIMIT 1
            """;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            if (!list.isEmpty()) {
                Map<String, Object> row = list.get(0);
                Integer id = (Integer) row.get("id");
                String name = (String) row.get("full_name");
                String email = (String) row.get("email");
                String phone = (String) row.get("phone");
                return AiChatResponse.StaffInfo.builder()
                        .id(id)
                        .name(name != null ? name : "Trần Hoàng Nam")
                        .role("Chuyên viên tư vấn & CSKH TechZone")
                        .email(email != null ? email : "support@techzone.vn")
                        .phone(phone != null ? phone : "1900 8888")
                        .avatar("👨‍💼")
                        .status("online")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm nhân viên online: {}", e.getMessage());
        }

        return AiChatResponse.StaffInfo.builder()
                .id(1)
                .name("Trần Hoàng Nam")
                .role("Chuyên viên tư vấn & Kỹ thuật TechZone")
                .email("support@techzone.vn")
                .phone("1900 8888")
                .avatar("👨‍💼")
                .status("online")
                .build();
    }

    public String mapStatusVietnamese(String status) {
        if (status == null) return "Chờ xử lý";
        return switch (status.toLowerCase()) {
            case "pending" -> "Chờ xác nhận";
            case "confirmed" -> "Đã xác nhận";
            case "preparing" -> "Đang đóng gói linh kiện";
            case "ready_for_pickup" -> "Đã tạo vận đơn GHN, chờ shipper lấy hàng";
            case "delivering" -> "Shipper GHN đang đi giao";
            case "completed" -> "Đã giao thành công";
            case "canceled" -> "Đã hủy đơn";
            default -> status;
        };
    }

    public String mapServiceStatusVietnamese(String status) {
        if (status == null) return "Đang xử lý";
        return switch (status.trim().toLowerCase()) {
            case "received" -> "Đã tiếp nhận thiết bị tại quầy kỹ thuật";
            case "inspecting" -> "Kỹ thuật viên đang kiểm tra & chẩn đoán lỗi";
            case "repairing" -> "Đang tiến hành sửa chữa / xử lý";
            case "waiting_for_parts" -> "Đang chờ linh kiện thay thế từ hãng";
            case "ready_for_return" -> "Đã xử lý xong, sẵn sàng trả máy cho khách";
            case "returned" -> "Đã bàn giao lại thiết bị cho khách";
            case "canceled" -> "Đã hủy phiếu tiếp nhận";
            default -> status;
        };
    }
}
