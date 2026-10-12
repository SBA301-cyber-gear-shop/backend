package com.project.cybershop.aftersales.internal.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thông tin đơn hàng và khách hàng của thẻ bảo hành, chỉ đọc.
 */
@Repository
public class OrderLookup {

    public record OrderInfo(Integer cardId, Integer orderItemId, String productName, String variantName, String sku,
                            Integer warrantyPeriodMonths, Integer orderId, String orderCode, String pickupName,
                            String pickupPhone, Integer userId, String customerName, String customerPhone) {
    }

    // shortcut: đọc thẳng bảng của module order và identity vì order chưa có API, thay bằng OrderApi khi có.
    private static final String FROM = """
            FROM "Warranty_Cards" wc
            JOIN "Order_Items" oi ON oi.id = wc.order_item_id
            JOIN "Orders" o ON o.id = oi.order_id
            JOIN "Users" u ON u.id = o.user_id
            """;

    private final EntityManager em;

    public OrderLookup(EntityManager em) {
        this.em = em;
    }

    public Map<Integer, OrderInfo> byCardIds(Collection<Integer> cardIds) {
        Map<Integer, OrderInfo> result = new LinkedHashMap<>();
        if (cardIds.isEmpty()) {
            return result;
        }
        List<?> rows = em.createNativeQuery("""
                        SELECT wc.id AS card_id, oi.id AS order_item_id, oi.product_name, oi.variant_name, oi.sku,
                               oi.warranty_period_months, o.id AS order_id, o.order_code, o.pickup_name,
                               o.pickup_phone, u.id AS user_id, u.full_name, u.phone
                        """ + FROM + "WHERE wc.id IN (:ids)")
                .setParameter("ids", cardIds)
                .getResultList();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            OrderInfo info = new OrderInfo(number(r[0]), number(r[1]), (String) r[2], (String) r[3], (String) r[4],
                    number(r[5]), number(r[6]), (String) r[7], (String) r[8], (String) r[9], number(r[10]),
                    (String) r[11], (String) r[12]);
            result.put(info.cardId(), info);
        }
        return result;
    }

    /** So khớp chính xác theo serial, mã đơn, số điện thoại hoặc mã phiếu để không gắn nhầm thẻ. */
    public List<Integer> searchCardIds(String query) {
        return ids(em.createNativeQuery("SELECT wc.id AS card_id " + FROM + """
                        WHERE upper(wc.serial_number) = :q OR upper(o.order_code) = :q
                           OR u.phone = :q OR o.pickup_phone = :q
                           OR EXISTS (SELECT 1 FROM "Service_requests" sr
                                      WHERE sr.warranty_card_id = wc.id AND upper(sr.request_code) = :q)
                        ORDER BY wc.id
                        """)
                .setParameter("q", query.trim().toUpperCase(Locale.ROOT))
                .getResultList());
    }

    public List<Integer> cardIdsOfUser(Integer userId) {
        return ids(em.createNativeQuery("SELECT wc.id AS card_id " + FROM + "WHERE o.user_id = :userId ORDER BY wc.id DESC")
                .setParameter("userId", userId)
                .getResultList());
    }

    private static List<Integer> ids(List<?> rows) {
        return rows.stream().map(OrderLookup::number).toList();
    }

    private static Integer number(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
