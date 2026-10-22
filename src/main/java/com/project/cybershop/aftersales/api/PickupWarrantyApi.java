package com.project.cybershop.aftersales.api;

import java.time.LocalDateTime;
import java.util.List;

public interface PickupWarrantyApi {
    record Unit(Integer orderItemId, String serialNumber, int warrantyMonths) {}
    record Card(String id, String orderId, Integer userId, String productId, String productName,
                String serial, LocalDateTime start, LocalDateTime end, String status) {}
    void issue(List<Unit> units, LocalDateTime start);
    List<Card> forCustomer(Integer userId);
}
