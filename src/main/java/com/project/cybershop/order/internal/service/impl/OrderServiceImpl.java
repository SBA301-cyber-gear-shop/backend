package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.aftersales.api.PickupWarrantyApi;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.order.api.dto.request.*;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import com.project.cybershop.order.internal.service.*;
import com.project.cybershop.payment.api.PaymentApi;
import com.project.cybershop.promotion.api.PromotionApi;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.*;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final JdbcTemplate jdbc;
    private final IdentityApi identity;
    private final CheckoutDetailsService details;
    private final PromotionApi promotions;
    private final PaymentApi payments;
    private final PickupWarrantyApi warranties;
    private final Clock clock;

    public OrderServiceImpl(JdbcTemplate jdbc, IdentityApi identity, CheckoutDetailsService details,
                            PromotionApi promotions, PaymentApi payments, PickupWarrantyApi warranties, Clock clock) {
        this.jdbc = jdbc; this.identity = identity; this.details = details;
        this.promotions = promotions; this.payments = payments; this.warranties = warranties; this.clock=clock;
    }

    @Override
    public OrderResponse place(String email, String key, PlaceOrderRequest request) {
        var user = actor(email, false);
        if (!"Customer".equalsIgnoreCase(user.getRole())) throw new AccessDeniedException("Chỉ tài khoản khách hàng được đặt hàng.");
        String code;
        try { code = "TZ-" + UUID.fromString(key).toString().replace("-", "").toUpperCase(Locale.ROOT); }
        catch (RuntimeException error) { throw new BusinessException("Idempotency-Key phải là UUID hợp lệ."); }
        // Shared with cart mutations: a cart cannot change halfway through checkout.
        if (jdbc.queryForList("SELECT id FROM users WHERE id=? AND is_active=true FOR UPDATE", user.getId()).isEmpty())
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Tài khoản đã bị khóa.");
        var previous = jdbc.queryForList("SELECT id,user_id FROM orders WHERE order_code=?", code);
        if (!previous.isEmpty()) {
            if (((Number) previous.getFirst().get("user_id")).intValue() != user.getId()) throw missing();
            return read(code, user.getId());
        }
        if (!"store".equals(request.method())) throw new BusinessException("Hiện hỗ trợ thanh toán tại cửa hàng. Cổng thanh toán online chưa được cấu hình.");
        if (request.paymentMethodId() != null) {
            var owned = jdbc.queryForList("SELECT id FROM payment_methods WHERE id=? AND user_id=?", request.paymentMethodId(), user.getId());
            if (owned.isEmpty()) throw new BusinessException("Phương thức thanh toán không thuộc tài khoản của bạn.");
            throw new BusinessException("Vui lòng chọn thanh toán tại cửa hàng.");
        }
        var carts = jdbc.queryForList("SELECT id FROM carts WHERE user_id=? FOR UPDATE", Integer.class, user.getId());
        if (carts.isEmpty()) throw new BusinessException("Giỏ hàng đang trống.");
        int cartId = carts.getFirst();
        var rows = jdbc.query("SELECT product_variant_id,quantity FROM cart_items WHERE cart_id=? ORDER BY product_variant_id,id FOR UPDATE",
                (rs, i) -> new CartLine(rs.getInt(1), rs.getInt(2)), cartId);
        if (rows.isEmpty()) throw new BusinessException("Giỏ hàng đang trống.");
        Map<Integer, Integer> quantities = new TreeMap<>();
        for (var row : rows) {
            if (row.quantity <= 0) throw new BusinessException("Số lượng trong giỏ hàng không hợp lệ.");
            quantities.merge(row.variant, row.quantity, Math::addExact);
        }
        // Lock in stable order to avoid deadlocks between orders sharing variants.
        for (int variant : quantities.keySet()) jdbc.queryForList("""
                SELECT pv.id FROM product_variants pv JOIN products p ON p.id=pv.product_id
                WHERE pv.id=? FOR UPDATE
                """, variant);
        var preview = details.preview(email, new CheckoutDetailsRequest(request.pickupName(), request.pickupPhone(), request.note(), request.promotionCode()));
        var voucher = promotions.quote(user.getId(), request.promotionCode(), preview.totalAmount(), true);
        if (preview.totalAmount().compareTo(request.expectedTotal()) != 0 || voucher.discount().compareTo(request.expectedDiscount()) != 0)
            throw new BusinessException("Giá hoặc ưu đãi đã thay đổi. Vui lòng kiểm tra lại trước khi xác nhận.");
        BigDecimal finalAmount = preview.totalAmount().subtract(voucher.discount());
        int orderId = insertOrder(user.getId(), code, preview.pickupName(), preview.pickupPhone(), preview.note(), preview.totalAmount(), voucher.discount(), finalAmount);
        for (var entry : quantities.entrySet()) {
            var snapshot = jdbc.query("""
                    SELECT p.name,pv.variant_name,pv.price,p.warranty_period_months,p.status,pv.stock_quantity
                    FROM product_variants pv JOIN products p ON p.id=pv.product_id WHERE pv.id=?
                    """, (rs, i) -> new Snapshot(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getInt(4), rs.getString(5), rs.getInt(6)), entry.getKey());
            if (snapshot.size() != 1) throw new BusinessException("Phiên bản sản phẩm không còn tồn tại.");
            var product = snapshot.getFirst();
            if (!"active".equalsIgnoreCase(product.status) || product.stock < entry.getValue() || product.warranty < 0)
                throw new BusinessException("Sản phẩm không còn đủ điều kiện đặt mua: " + product.name);
            int changed = jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity-? WHERE id=? AND stock_quantity>=?",
                    entry.getValue(), entry.getKey(), entry.getValue());
            if (changed != 1) throw new BusinessException("Không đủ tồn kho: " + product.name);
            String name = product.name + (product.variant == null || product.variant.isBlank() ? "" : " · " + product.variant);
            jdbc.update("""
                    INSERT INTO order_items (order_id,product_variant_id,product_name,quantity,unit_price,subtotal,warranty_period_months)
                    VALUES (?,?,?,?,?,?,?)
                    """, orderId, entry.getKey(), name, entry.getValue(), product.price,
                    product.price.multiply(BigDecimal.valueOf(entry.getValue())), product.warranty);
        }
        promotions.reserve(voucher, user.getId(), orderId, finalAmount.signum() == 0);
        payments.createCounterPayment(orderId, finalAmount);
        jdbc.update("DELETE FROM cart_items WHERE cart_id=?", cartId);
        jdbc.update("UPDATE carts SET updated_at=? WHERE id=?", LocalDateTime.now(clock), cartId);
        return read(code, user.getId());
    }

    private int insertOrder(int userId, String code, String name, String phone, String note,
                            BigDecimal total, BigDecimal discount, BigDecimal amount) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO orders (user_id,order_code,total_amount,discount_amount,final_amount,status,pickup_name,pickup_phone,note,created_at,updated_at)
                    VALUES (?,?,?,?,?,'pending',?,?,?,?,?)
                    """, new String[]{"id"});
            Object[] args = {userId, code, total, discount, amount, name, phone, note, LocalDateTime.now(clock), LocalDateTime.now(clock)};
            for (int i=0; i<args.length; i++) statement.setObject(i+1, args[i]);
            return statement;
        }, key);
        if (key.getKey() == null) throw new IllegalStateException("Không lấy được mã đơn hàng.");
        return key.getKey().intValue();
    }

    @Override
    @Transactional(readOnly=true)
    public List<OrderResponse> myOrders(String email) {
        var user = actor(email, false);
        return jdbc.queryForList("SELECT order_code FROM orders WHERE user_id=? ORDER BY id DESC", String.class, user.getId())
                .stream().map(code -> read(code, user.getId())).toList();
    }

    @Override
    @Transactional(readOnly=true)
    public OrderResponse myOrder(String email, String code) { return read(code, actor(email, false).getId()); }

    @Override
    @Transactional(readOnly=true)
    public List<OrderResponse> operations(String email, String search, String status) {
        actor(email, true);
        String selectedStatus = status == null || "all".equals(status) ? "" : status;
        String term = "%" + (search == null ? "" : search.trim().toLowerCase(Locale.ROOT)) + "%";
        return jdbc.queryForList("""
                SELECT order_code FROM orders WHERE (?='' OR status=?)
                AND (lower(order_code) LIKE ? OR lower(COALESCE(pickup_name,'')) LIKE ? OR COALESCE(pickup_phone,'') LIKE ?)
                ORDER BY id DESC LIMIT 200
                """, String.class, selectedStatus, selectedStatus, term, term, term).stream().map(code -> read(code, null)).toList();
    }

    @Override
    public OrderResponse advance(String email, String code, String nextStatus) {
        actor(email, true);
        var order = locked(code, null);
        if (order.status().equals(nextStatus)) return order;
        String expected = switch (order.status()) {
            case "pending" -> "confirmed";
            case "confirmed" -> "preparing";
            case "preparing" -> "ready_for_pickup";
            default -> "";
        };
        if (!expected.equals(nextStatus)) throw new BusinessException("Không được bỏ qua hoặc quay lại bước xử lý đơn.");
        jdbc.update("UPDATE orders SET status=?,updated_at=? WHERE id=?", nextStatus, LocalDateTime.now(clock), order.id());
        return read(code, null);
    }

    @Override
    public OrderResponse collect(String email, String code, CollectPaymentRequest request) {
        actor(email, true);
        var order = locked(code, null);
        if (Set.of("canceled","completed").contains(order.status())) throw new BusinessException("Đơn không còn được thu tiền.");
        if (payments.get(order.id()).amount().compareTo(order.finalAmount()) != 0) throw new BusinessException("Khoản thanh toán không khớp giá trị đơn hàng.");
        payments.collect(order.id(), request.receivedAmount(), request.reference());
        promotions.markUsed(order.id());
        jdbc.update("UPDATE orders SET updated_at=? WHERE id=?", LocalDateTime.now(clock), order.id());
        return read(code, null);
    }

    @Override
    public OrderResponse cancel(String email, String code, boolean staff) {
        var user = actor(email, staff);
        var order = locked(code, staff ? null : user.getId());
        if ("canceled".equals(order.status())) return order;
        if ("completed".equals(order.status()) || (!staff && !Set.of("pending","confirmed").contains(order.status())))
            throw new BusinessException("Đơn không còn được hủy tại bước này.");
        payments.cancel(order.id());
        for (var item : order.items().stream().sorted(Comparator.comparing(OrderResponse.Item::productVariantId)).toList())
            jdbc.update("UPDATE product_variants SET stock_quantity=stock_quantity+? WHERE id=?", item.quantity(), item.productVariantId());
        promotions.release(order.id());
        jdbc.update("UPDATE orders SET status='canceled',updated_at=? WHERE id=?", LocalDateTime.now(clock), order.id());
        return read(code, staff ? null : user.getId());
    }

    @Override
    public OrderResponse handover(String email, String code, HandoverRequest request) {
        actor(email, true);
        var order = locked(code, null);
        if (!"ready_for_pickup".equals(order.status()) || !"SUCCESS".equals(order.paymentStatus()))
            throw new BusinessException("Đơn phải sẵn sàng nhận và đã thanh toán đủ trước khi bàn giao.");
        if (payments.get(order.id()).amount().compareTo(order.finalAmount()) != 0) throw new BusinessException("Khoản thanh toán không khớp giá trị đơn hàng.");
        if (!request.recipientVerified() || !digits(order.pickupPhone()).equals(digits(request.recipientPhone())))
            throw new BusinessException("Thông tin người nhận chưa được đối chiếu chính xác.");
        Map<Integer, Long> supplied = new HashMap<>();
        Set<String> serials = new HashSet<>();
        List<PickupWarrantyApi.Unit> units = new ArrayList<>();
        for (var device : request.devices()) {
            var item = order.items().stream().filter(line -> line.id().equals(device.orderItemId()) && line.warrantyPeriodMonths()>0)
                    .findFirst().orElseThrow(() -> new BusinessException("Serial không thuộc dòng hàng cần bảo hành của đơn này."));
            String serial = device.serialNumber().trim().toUpperCase(Locale.ROOT);
            if (serial.isEmpty() || !serials.add(serial)) throw new BusinessException("Serial từng thiết bị phải đầy đủ và không trùng nhau.");
            supplied.merge(item.id(), 1L, Long::sum);
            units.add(new PickupWarrantyApi.Unit(item.id(), serial, item.warrantyPeriodMonths()));
        }
        for (var item : order.items()) if (item.warrantyPeriodMonths()>0 && supplied.getOrDefault(item.id(), 0L) != item.quantity())
            throw new BusinessException("Cần nhập đủ serial cho từng thiết bị được bảo hành.");
        var now = LocalDateTime.now(clock);
        warranties.issue(units, now);
        jdbc.update("UPDATE orders SET status='completed',updated_at=? WHERE id=?", now, order.id());
        return read(code, null);
    }

    private static String digits(String phone) { return phone == null ? "" : phone.replaceAll("[^0-9]", ""); }

    private UserDto actor(String email, boolean staff) {
        var user = identity.requireActiveUser(email);
        if (staff && !Set.of("staff","admin").contains(user.getRole().toLowerCase(Locale.ROOT)))
            throw new AccessDeniedException("Chỉ nhân viên bán hàng hoặc quản trị viên được xử lý đơn.");
        return user;
    }

    private OrderResponse locked(String code, Integer userId) {
        var ids = jdbc.queryForList("SELECT id FROM orders WHERE order_code=?" + (userId == null ? "" : " AND user_id=?") + " FOR UPDATE",
                Integer.class, userId == null ? new Object[]{code} : new Object[]{code,userId});
        if (ids.isEmpty()) throw missing();
        return read(code, userId);
    }

    private OrderResponse read(String code, Integer userId) {
        var orders = jdbc.query("SELECT * FROM orders WHERE order_code=?" + (userId == null ? "" : " AND user_id=?"), (rs, i) -> {
            int id = rs.getInt("id");
            var payment = payments.get(id);
            var promotionsUsed = jdbc.queryForList("SELECT p.code FROM promotion_usages u JOIN promotions p ON p.id=u.promotion_id WHERE u.order_id=?", String.class, id);
            var items = jdbc.query("""
                    SELECT oi.*,r.rating,r.comment AS review_comment FROM order_items oi
                    LEFT JOIN reviews r ON r.order_item_id=oi.id WHERE oi.order_id=? ORDER BY oi.id
                    """, (line, index) -> new OrderResponse.Item(line.getInt("id"), line.getInt("product_variant_id"), line.getString("product_name"),
                    line.getInt("quantity"), line.getBigDecimal("unit_price"), line.getBigDecimal("subtotal"), line.getInt("warranty_period_months"),
                    (Integer) line.getObject("rating"), line.getString("review_comment")), id);
            return new OrderResponse(id, rs.getString("order_code"), rs.getInt("user_id"), rs.getString("status"),
                    rs.getString("pickup_name"), rs.getString("pickup_phone"), rs.getString("note"), rs.getBigDecimal("total_amount"),
                    rs.getBigDecimal("discount_amount"), rs.getBigDecimal("final_amount"), promotionsUsed.isEmpty() ? null : promotionsUsed.getFirst(),
                    payment.status(), payment.methodType(), rs.getObject("created_at", LocalDateTime.class),
                    rs.getObject("updated_at", LocalDateTime.class), payment.paidAt(), items);
        }, userId == null ? new Object[]{code} : new Object[]{code,userId});
        if (orders.isEmpty()) throw missing();
        return orders.getFirst();
    }

    private static ResourceNotFoundException missing() { return new ResourceNotFoundException("Không tìm thấy đơn hàng."); }
    private record CartLine(int variant, int quantity) {}
    private record Snapshot(String name, String variant, BigDecimal price, int warranty, String status, int stock) {}
}
