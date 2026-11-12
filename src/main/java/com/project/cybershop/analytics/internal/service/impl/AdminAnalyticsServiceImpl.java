package com.project.cybershop.analytics.internal.service.impl;

import com.project.cybershop.analytics.api.dto.DashboardSummaryResponse;
import com.project.cybershop.analytics.internal.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final JdbcTemplate jdbc;

    @Override
    public DashboardSummaryResponse getDashboardSummary(Integer months, Integer days) {
        int m = (months != null && months > 0) ? months : 12;
        int d = (days != null && days > 0) ? days : 14;

        DashboardSummaryResponse.KpiSummaryDto kpis = calculateKpis();
        List<DashboardSummaryResponse.MonthlyRevenueDto> monthlyRevenue = getMonthlyRevenue(m);
        List<DashboardSummaryResponse.DailyOrderStatsDto> dailyOrders = getDailyOrders(d);
        List<DashboardSummaryResponse.MonthlyOrderStatsDto> monthlyOrders = getMonthlyOrders(m);
        List<DashboardSummaryResponse.MonthlyWarrantyDto> monthlyWarranties = getMonthlyWarranties(m);
        List<DashboardSummaryResponse.MonthlyUserGrowthDto> monthlyUsers = getMonthlyUsers(m);
        List<DashboardSummaryResponse.PromotionAnalyticsDto> promotions = getPromotionStats();
        List<DashboardSummaryResponse.TopSellingProductDto> topProducts = getTopSellingProducts(10);

        return new DashboardSummaryResponse(
                kpis,
                monthlyRevenue,
                dailyOrders,
                monthlyOrders,
                monthlyWarranties,
                monthlyUsers,
                promotions,
                topProducts
        );
    }

    private DashboardSummaryResponse.KpiSummaryDto calculateKpis() {
        // Orders & Revenue
        String orderSql = """
                SELECT 
                    COUNT(*) AS total_orders,
                    COUNT(CASE WHEN LOWER(status) = 'canceled' THEN 1 END) AS canceled_orders,
                    COALESCE(SUM(CASE WHEN LOWER(status) != 'canceled' THEN final_amount ELSE 0 END), 0) AS total_revenue
                FROM orders
                """;
        Map<String, Object> orderMap = jdbc.queryForMap(orderSql);
        long totalOrders = toLong(orderMap.get("total_orders"));
        long canceledOrders = toLong(orderMap.get("canceled_orders"));
        BigDecimal totalRevenue = toBigDecimal(orderMap.get("total_revenue"));
        double cancelRate = totalOrders > 0 ? (canceledOrders * 100.0 / totalOrders) : 0.0;
        cancelRate = Math.round(cancelRate * 10.0) / 10.0;

        // Items sold
        String itemSql = """
                SELECT COALESCE(SUM(oi.quantity), 0) AS items_sold
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                WHERE LOWER(o.status) != 'canceled'
                """;
        long totalItemsSold = toLong(jdbc.queryForObject(itemSql, Long.class));

        // Users
        String userSql = """
                SELECT 
                    COUNT(*) AS total_users,
                    COUNT(CASE WHEN UPPER(coalesce(r.name, '')) = 'CUSTOMER' THEN 1 END) AS total_customers
                FROM users u
                LEFT JOIN roles r ON u.role_id = r.id
                """;
        Map<String, Object> userMap = jdbc.queryForMap(userSql);
        long totalUsers = toLong(userMap.get("total_users"));
        long totalCustomers = toLong(userMap.get("total_customers"));

        // Warranties
        String warrantySql = """
                SELECT 
                    COUNT(*) AS total_requests,
                    COUNT(CASE WHEN LOWER(status) NOT IN ('returned', 'canceled') THEN 1 END) AS active_requests
                FROM service_requests
                """;
        Map<String, Object> warrantyMap = jdbc.queryForMap(warrantySql);
        long totalWarrantyRequests = toLong(warrantyMap.get("total_requests"));
        long activeWarrantyRequests = toLong(warrantyMap.get("active_requests"));

        // Promotions
        String promoSql = """
                SELECT 
                    (SELECT COUNT(*) FROM promotions) AS total_promotions,
                    (SELECT COUNT(*) FROM promotion_usages WHERE status IS NULL OR UPPER(status) NOT IN ('REVOKED', 'CANCELED')) AS total_uses,
                    (SELECT COALESCE(SUM(discount_amount), 0) FROM promotion_usages WHERE status IS NULL OR UPPER(status) NOT IN ('REVOKED', 'CANCELED')) AS total_discount
                """;
        Map<String, Object> promoMap = jdbc.queryForMap(promoSql);
        long totalPromotions = toLong(promoMap.get("total_promotions"));
        long totalVoucherUses = toLong(promoMap.get("total_uses"));
        BigDecimal totalDiscountGiven = toBigDecimal(promoMap.get("total_discount"));

        return new DashboardSummaryResponse.KpiSummaryDto(
                totalRevenue,
                totalItemsSold,
                totalOrders,
                canceledOrders,
                cancelRate,
                totalUsers,
                totalCustomers,
                totalWarrantyRequests,
                activeWarrantyRequests,
                totalPromotions,
                totalVoucherUses,
                totalDiscountGiven
        );
    }

    @Override
    public List<DashboardSummaryResponse.DailyOrderStatsDto> getDailyOrders(int days) {
        String sql = """
                SELECT 
                    TO_CHAR(created_at, 'YYYY-MM-DD') AS report_date,
                    COUNT(*) AS total_orders,
                    COUNT(CASE WHEN LOWER(status) = 'completed' THEN 1 END) AS completed_orders,
                    COUNT(CASE WHEN LOWER(status) = 'canceled' THEN 1 END) AS canceled_orders,
                    COUNT(CASE WHEN LOWER(status) NOT IN ('completed', 'canceled') THEN 1 END) AS processing_orders
                FROM orders
                WHERE created_at IS NOT NULL
                  AND created_at >= NOW() - make_interval(days => ?)
                GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD')
                ORDER BY report_date ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new DashboardSummaryResponse.DailyOrderStatsDto(
                rs.getString("report_date"),
                rs.getLong("total_orders"),
                rs.getLong("completed_orders"),
                rs.getLong("canceled_orders"),
                rs.getLong("processing_orders")
        ), days);
    }

    @Override
    public List<DashboardSummaryResponse.MonthlyOrderStatsDto> getMonthlyOrders(int months) {
        String sql = """
                SELECT 
                    TO_CHAR(created_at, 'YYYY-MM') AS report_month,
                    COUNT(*) AS total_orders,
                    COUNT(CASE WHEN LOWER(status) = 'completed' THEN 1 END) AS completed_orders,
                    COUNT(CASE WHEN LOWER(status) = 'canceled' THEN 1 END) AS canceled_orders,
                    COUNT(CASE WHEN LOWER(status) NOT IN ('completed', 'canceled') THEN 1 END) AS processing_orders
                FROM orders
                WHERE created_at IS NOT NULL
                  AND created_at >= NOW() - make_interval(months => ?)
                GROUP BY TO_CHAR(created_at, 'YYYY-MM')
                ORDER BY report_month ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new DashboardSummaryResponse.MonthlyOrderStatsDto(
                rs.getString("report_month"),
                rs.getLong("total_orders"),
                rs.getLong("completed_orders"),
                rs.getLong("canceled_orders"),
                rs.getLong("processing_orders")
        ), months);
    }

    @Override
    public List<DashboardSummaryResponse.MonthlyRevenueDto> getMonthlyRevenue(int months) {
        int m = months > 0 ? months : 12;
        String sql = """
                SELECT 
                    sub.report_month,
                    COALESCE(SUM(sub.final_amount), 0) AS revenue,
                    COALESCE(SUM(sub.order_item_qty), 0) AS items_sold,
                    COUNT(DISTINCT sub.order_id) AS order_count
                FROM (
                    SELECT 
                        o.id AS order_id,
                        TO_CHAR(o.created_at, 'YYYY-MM') AS report_month,
                        o.final_amount,
                        COALESCE(SUM(oi.quantity), 0) AS order_item_qty
                    FROM orders o
                    LEFT JOIN order_items oi ON o.id = oi.order_id
                    WHERE o.created_at IS NOT NULL
                      AND LOWER(o.status) != 'canceled'
                      AND o.created_at >= NOW() - make_interval(months => ?)
                    GROUP BY o.id, TO_CHAR(o.created_at, 'YYYY-MM'), o.final_amount
                ) sub
                GROUP BY sub.report_month
                ORDER BY sub.report_month ASC
                """;
        List<DashboardSummaryResponse.MonthlyRevenueDto> queryResults = jdbc.query(sql, (rs, rowNum) -> new DashboardSummaryResponse.MonthlyRevenueDto(
                rs.getString("report_month"),
                rs.getBigDecimal("revenue"),
                rs.getLong("items_sold"),
                rs.getLong("order_count")
        ), m);

        Map<String, DashboardSummaryResponse.MonthlyRevenueDto> resultMap = new HashMap<>();
        for (DashboardSummaryResponse.MonthlyRevenueDto item : queryResults) {
            resultMap.put(item.month(), item);
        }

        List<DashboardSummaryResponse.MonthlyRevenueDto> fullList = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = m - 1; i >= 0; i--) {
            String monthKey = current.minusMonths(i).toString();
            if (resultMap.containsKey(monthKey)) {
                fullList.add(resultMap.get(monthKey));
            } else {
                fullList.add(new DashboardSummaryResponse.MonthlyRevenueDto(
                        monthKey,
                        BigDecimal.ZERO,
                        0L,
                        0L
                ));
            }
        }
        return fullList;
    }

    @Override
    public List<DashboardSummaryResponse.MonthlyWarrantyDto> getMonthlyWarranties(int months) {
        String sql = """
                SELECT 
                    TO_CHAR(created_at, 'YYYY-MM') AS report_month,
                    COUNT(*) AS total_requests,
                    COUNT(CASE WHEN LOWER(status) = 'returned' THEN 1 END) AS completed_requests,
                    COUNT(CASE WHEN LOWER(status) NOT IN ('returned', 'canceled') THEN 1 END) AS processing_requests,
                    COUNT(CASE WHEN LOWER(status) = 'canceled' THEN 1 END) AS canceled_requests
                FROM service_requests
                WHERE created_at IS NOT NULL
                  AND created_at >= NOW() - make_interval(months => ?)
                GROUP BY TO_CHAR(created_at, 'YYYY-MM')
                ORDER BY report_month ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new DashboardSummaryResponse.MonthlyWarrantyDto(
                rs.getString("report_month"),
                rs.getLong("total_requests"),
                rs.getLong("completed_requests"),
                rs.getLong("processing_requests"),
                rs.getLong("canceled_requests")
        ), months);
    }

    @Override
    public List<DashboardSummaryResponse.MonthlyUserGrowthDto> getMonthlyUsers(int months) {
        String sql = """
                SELECT 
                    TO_CHAR(u.created_at, 'YYYY-MM') AS report_month,
                    COUNT(*) AS new_users,
                    COUNT(CASE WHEN UPPER(coalesce(r.name, '')) = 'CUSTOMER' THEN 1 END) AS new_customers,
                    COUNT(CASE WHEN UPPER(coalesce(r.name, '')) != 'CUSTOMER' THEN 1 END) AS new_staff
                FROM users u
                LEFT JOIN roles r ON u.role_id = r.id
                WHERE u.created_at IS NOT NULL
                  AND u.created_at >= NOW() - make_interval(months => ?)
                GROUP BY TO_CHAR(u.created_at, 'YYYY-MM')
                ORDER BY report_month ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new DashboardSummaryResponse.MonthlyUserGrowthDto(
                rs.getString("report_month"),
                rs.getLong("new_users"),
                rs.getLong("new_customers"),
                rs.getLong("new_staff")
        ), months);
    }

    @Override
    public List<DashboardSummaryResponse.PromotionAnalyticsDto> getPromotionStats() {
        String sql = """
                SELECT 
                    p.id,
                    p.code,
                    p.name,
                    p.discount_type,
                    p.discount_value,
                    p.usage_limit,
                    p.audience,
                    p.is_active,
                    TO_CHAR(p.start_date, 'YYYY-MM-DD HH24:MI') AS start_date_str,
                    TO_CHAR(p.end_date, 'YYYY-MM-DD HH24:MI') AS end_date_str,
                    COUNT(pu.id) AS usage_count,
                    COALESCE(SUM(pu.discount_amount), 0) AS total_discount_amount
                FROM promotions p
                LEFT JOIN promotion_usages pu ON p.id = pu.promotion_id 
                    AND (pu.status IS NULL OR UPPER(pu.status) NOT IN ('REVOKED', 'CANCELED'))
                GROUP BY p.id, p.code, p.name, p.discount_type, p.discount_value, p.usage_limit, 
                         p.audience, p.is_active, p.start_date, p.end_date
                ORDER BY usage_count DESC, total_discount_amount DESC
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            int limit = rs.getInt("usage_limit");
            long usage = rs.getLong("usage_count");
            double usageRate = limit > 0 ? (usage * 100.0 / limit) : 0.0;
            usageRate = Math.round(usageRate * 10.0) / 10.0;

            return new DashboardSummaryResponse.PromotionAnalyticsDto(
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("discount_type"),
                    rs.getBigDecimal("discount_value"),
                    limit,
                    rs.getString("audience"),
                    rs.getBoolean("is_active"),
                    rs.getString("start_date_str"),
                    rs.getString("end_date_str"),
                    usage,
                    rs.getBigDecimal("total_discount_amount"),
                    usageRate
            );
        });
    }

    @Override
    public List<DashboardSummaryResponse.TopSellingProductDto> getTopSellingProducts(int limit) {
        String sql = """
                SELECT 
                    oi.product_name,
                    COALESCE(MAX(c.name), 'Linh kiện') AS category_name,
                    SUM(oi.quantity) AS quantity_sold,
                    SUM(oi.subtotal) AS total_revenue
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                LEFT JOIN product_variants pv ON oi.product_variant_id = pv.id
                LEFT JOIN products p ON pv.product_id = p.id
                LEFT JOIN categories c ON p.category_id = c.id
                WHERE LOWER(o.status) != 'canceled'
                GROUP BY oi.product_name
                ORDER BY quantity_sold DESC, total_revenue DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, rowNum) -> new DashboardSummaryResponse.TopSellingProductDto(
                rs.getString("product_name"),
                rs.getString("category_name"),
                rs.getLong("quantity_sold"),
                rs.getBigDecimal("total_revenue")
        ), limit);
    }

    private static long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number num) return num.longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

