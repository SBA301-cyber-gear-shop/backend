package com.project.cybershop.analytics.internal.controller;

import com.project.cybershop.analytics.api.dto.DashboardSummaryResponse;
import com.project.cybershop.analytics.internal.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin - Analytics & Statistics", description = "Thống kê số liệu doanh thu, đơn hàng, bảo hành, người dùng và khuyến mãi cho Admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Lấy dữ liệu tổng hợp toàn diện cho Dashboard Admin (KPI, doanh thu, đơn hàng, bảo hành, người dùng, voucher)")
    public DashboardSummaryResponse getSummary(
            @RequestParam(required = false, defaultValue = "12") Integer months,
            @RequestParam(required = false, defaultValue = "14") Integer days
    ) {
        return analyticsService.getDashboardSummary(months, days);
    }

    @GetMapping("/daily-orders")
    @Operation(summary = "Thống kê đơn hàng theo ngày và số đơn bị hủy")
    public List<DashboardSummaryResponse.DailyOrderStatsDto> getDailyOrders(
            @RequestParam(required = false, defaultValue = "30") int days
    ) {
        return analyticsService.getDailyOrders(days);
    }

    @GetMapping("/monthly-orders")
    @Operation(summary = "Thống kê đơn hàng theo tháng và số đơn bị hủy")
    public List<DashboardSummaryResponse.MonthlyOrderStatsDto> getMonthlyOrders(
            @RequestParam(required = false, defaultValue = "12") int months
    ) {
        return analyticsService.getMonthlyOrders(months);
    }

    @GetMapping("/monthly-revenue")
    @Operation(summary = "Thống kê doanh thu theo tháng và số lượng hàng hoá đã bán được")
    public List<DashboardSummaryResponse.MonthlyRevenueDto> getMonthlyRevenue(
            @RequestParam(required = false, defaultValue = "12") int months
    ) {
        return analyticsService.getMonthlyRevenue(months);
    }

    @GetMapping("/monthly-warranties")
    @Operation(summary = "Thống kê bảo hành tiếp nhận và xử lý theo tháng")
    public List<DashboardSummaryResponse.MonthlyWarrantyDto> getMonthlyWarranties(
            @RequestParam(required = false, defaultValue = "12") int months
    ) {
        return analyticsService.getMonthlyWarranties(months);
    }

    @GetMapping("/monthly-users")
    @Operation(summary = "Thống kê tăng trưởng người dùng mới theo tháng")
    public List<DashboardSummaryResponse.MonthlyUserGrowthDto> getMonthlyUsers(
            @RequestParam(required = false, defaultValue = "12") int months
    ) {
        return analyticsService.getMonthlyUsers(months);
    }

    @GetMapping("/promotions")
    @Operation(summary = "Thống kê các chương trình giảm giá: số lượng dùng và tổng tiền đã giảm")
    public List<DashboardSummaryResponse.PromotionAnalyticsDto> getPromotions() {
        return analyticsService.getPromotionStats();
    }

    @GetMapping("/top-products")
    @Operation(summary = "Top sản phẩm bán chạy nhất theo số lượng và doanh thu")
    public List<DashboardSummaryResponse.TopSellingProductDto> getTopSellingProducts(
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return analyticsService.getTopSellingProducts(limit);
    }
}

