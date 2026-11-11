package com.project.cybershop.support.internal.model;

import java.util.List;

public record CustomerContext(
        Integer userId,
        String fullName,
        String phone,
        List<OrderSummary> recentOrders,
        List<WarrantySummary> activeWarranties,
        List<ServiceRequestSummary> activeServiceRequests
) {
    public boolean isMember() {
        return userId != null;
    }

    public record OrderSummary(
            Integer id,
            String code,
            String status,
            String deliveryType,
            String trackingCode,
            double finalAmount,
            String createdAt,
            String items
    ) {}

    public record WarrantySummary(
            Integer id,
            String serialNumber,
            String productName,
            String endDate,
            long daysRemaining,
            String status
    ) {}

    public record ServiceRequestSummary(
            Integer id,
            String requestCode,
            String serialNumber,
            String productName,
            String status,
            String statusVietnamese,
            String issueDescription,
            String expectedReturnDate
    ) {}
}
