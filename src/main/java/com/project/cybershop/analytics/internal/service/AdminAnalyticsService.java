package com.project.cybershop.analytics.internal.service;

import com.project.cybershop.analytics.api.dto.DashboardSummaryResponse;

import java.util.List;

public interface AdminAnalyticsService {

    DashboardSummaryResponse getDashboardSummary(Integer months, Integer days);

    List<DashboardSummaryResponse.DailyOrderStatsDto> getDailyOrders(int days);

    List<DashboardSummaryResponse.MonthlyOrderStatsDto> getMonthlyOrders(int months);

    List<DashboardSummaryResponse.MonthlyRevenueDto> getMonthlyRevenue(int months);

    List<DashboardSummaryResponse.MonthlyWarrantyDto> getMonthlyWarranties(int months);

    List<DashboardSummaryResponse.MonthlyUserGrowthDto> getMonthlyUsers(int months);

    List<DashboardSummaryResponse.PromotionAnalyticsDto> getPromotionStats();

    List<DashboardSummaryResponse.TopSellingProductDto> getTopSellingProducts(int limit);
}

