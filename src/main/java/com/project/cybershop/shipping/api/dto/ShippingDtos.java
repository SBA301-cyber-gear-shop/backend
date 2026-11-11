package com.project.cybershop.shipping.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class ShippingDtos {

    public record ProvinceDto(int id, String name) {}

    public record DistrictDto(int id, int provinceId, String name) {}

    public record WardDto(String code, int districtId, String name) {}

    public record CalculateFeeRequest(Integer toDistrictId, String toWardCode, Integer weightGram) {}

    public record CalculateFeeResponse(BigDecimal totalFee, String serviceName) {}

    public record TrackingLogItem(String time, String status, String description, String location) {}

    public record TrackingResponse(
            String orderCode,
            String trackingCode,
            String carrier,
            String status,
            String statusText,
            String expectedDeliveryTime,
            String printUrl,
            BigDecimal codAmount,
            BigDecimal shippingFee,
            List<TrackingLogItem> logs
    ) {}

    public record SwitchStatusRequest(String status, String note) {}

    public record UpdateShippingAddressRequest(
            String pickupName,
            String pickupPhone,
            String shippingAddress,
            Integer toProvinceId,
            Integer toDistrictId,
            String toWardCode
    ) {}
}
