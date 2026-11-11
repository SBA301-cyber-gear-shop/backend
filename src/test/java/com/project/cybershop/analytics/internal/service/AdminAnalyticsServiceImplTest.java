package com.project.cybershop.analytics.internal.service;

import com.project.cybershop.analytics.api.dto.DashboardSummaryResponse;
import com.project.cybershop.analytics.internal.service.impl.AdminAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceImplTest {

    @Mock
    private JdbcTemplate jdbc;

    private AdminAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminAnalyticsServiceImpl(jdbc);
    }

    @Test
    void testGetDashboardSummary() {
        // Mock order stats
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("total_orders", 100L);
        orderMap.put("canceled_orders", 5L);
        orderMap.put("total_revenue", new BigDecimal("50000000"));

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("total_users", 80L);
        userMap.put("total_customers", 75L);

        Map<String, Object> warrantyMap = new HashMap<>();
        warrantyMap.put("total_requests", 12L);
        warrantyMap.put("active_requests", 3L);

        Map<String, Object> promoMap = new HashMap<>();
        promoMap.put("total_promotions", 10L);
        promoMap.put("total_uses", 45L);
        promoMap.put("total_discount", new BigDecimal("4500000"));

        when(jdbc.queryForMap(contains("orders"))).thenReturn(orderMap);
        when(jdbc.queryForObject(contains("items_sold"), eq(Long.class))).thenReturn(250L);
        when(jdbc.queryForMap(contains("users"))).thenReturn(userMap);
        when(jdbc.queryForMap(contains("service_requests"))).thenReturn(warrantyMap);
        when(jdbc.queryForMap(contains("promotions"))).thenReturn(promoMap);

        DashboardSummaryResponse res = service.getDashboardSummary(12, 14);

        assertNotNull(res);
        assertNotNull(res.kpis());
        assertEquals(100L, res.kpis().totalOrders());
        assertEquals(5L, res.kpis().totalCanceledOrders());
        assertEquals(5.0, res.kpis().cancelRate());
        assertEquals(new BigDecimal("50000000"), res.kpis().totalRevenue());
        assertEquals(250L, res.kpis().totalItemsSold());
        assertEquals(80L, res.kpis().totalUsers());
        assertEquals(75L, res.kpis().totalCustomers());
        assertEquals(12L, res.kpis().totalWarrantyRequests());
        assertEquals(3L, res.kpis().activeWarrantyRequests());
        assertEquals(10L, res.kpis().totalPromotions());
        assertEquals(45L, res.kpis().totalVoucherUses());
        assertEquals(new BigDecimal("4500000"), res.kpis().totalDiscountGiven());
    }
}

