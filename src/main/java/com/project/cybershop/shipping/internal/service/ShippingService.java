package com.project.cybershop.shipping.internal.service;

import com.project.cybershop.shipping.api.dto.ShippingDtos.*;

import java.util.List;
import java.util.Map;

public interface ShippingService {
    List<ProvinceDto> getProvinces();
    List<DistrictDto> getDistricts(int provinceId);
    List<WardDto> getWards(int districtId);
    CalculateFeeResponse calculateFee(CalculateFeeRequest request);
    TrackingResponse createShipment(String orderCode, String actorEmail);
    TrackingResponse getTracking(String orderCode, String actorEmail);
    TrackingResponse switchStatus(String orderCode, String action, String actorEmail);
    String getPrintToken(String orderCode);
    void handleWebhook(Map<String, Object> payload);
    void updateShippingAddress(String orderCode, UpdateShippingAddressRequest request);
}
