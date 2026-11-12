package com.project.cybershop.analytics.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        KpiSummaryDto kpis,
        List<MonthlyRevenueDto> monthlyRevenue,
        List<DailyOrderStatsDto> dailyOrders,
        List<MonthlyOrderStatsDto> monthlyOrders,
        List<MonthlyWarrantyDto> monthlyWarranties,
        List<MonthlyUserGrowthDto> monthlyUsers,
        List<PromotionAnalyticsDto> promotions,
        List<TopSellingProductDto> topProducts
) {
    public record KpiSummaryDto(
            BigDecimal totalRevenue,
            long totalItemsSold,
            long totalOrders,
            long totalCanceledOrders,
            double cancelRate,
            long totalUsers,
            long totalCustomers,
            long totalWarrantyRequests,
            long activeWarrantyRequests,
            long totalPromotions,
            long totalVoucherUses,
            BigDecimal totalDiscountGiven
    ) {}

    public record MonthlyRevenueDto(
            String month,
            BigDecimal revenue,
            long itemsSold,
            long orderCount
    ) {}

    public record DailyOrderStatsDto(
            String date,
            long totalOrders,
            long completedOrders,
            long canceledOrders,
            long processingOrders
    ) {}

    public record MonthlyOrderStatsDto(
            String month,
            long totalOrders,
            long completedOrders,
            long canceledOrders,
            long processingOrders
    ) {}

    public record MonthlyWarrantyDto(
            String month,
            long totalRequests,
            long completedRequests,
            long processingRequests,
            long canceledRequests
    ) {}

    public record MonthlyUserGrowthDto(
            String month,
            long newUsers,
            long newCustomers,
            long newStaff
    ) {}

    public record PromotionAnalyticsDto(
            Integer id,
            String code,
            String name,
            String discountType,
            BigDecimal discountValue,
            Integer usageLimit,
            String audience,
            Boolean isActive,
            String startDate,
            String endDate,
            long usageCount,
            BigDecimal totalDiscountAmount,
            double usageRate
    ) {}

    public record TopSellingProductDto(
            String productName,
            String categoryName,
            long quantitySold,
            BigDecimal totalRevenue
    ) {}
}

