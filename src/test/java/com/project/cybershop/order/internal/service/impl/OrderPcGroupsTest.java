package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.order.api.dto.request.PlaceOrderRequest.PcGroup;
import com.project.cybershop.order.api.dto.request.PlaceOrderRequest.PcPart;
import com.project.cybershop.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderPcGroupsTest {
    private final PcGroup pc = new PcGroup(List.of(new PcPart(1, 1, "CPU"), new PcPart(2, 2, "RAM")));

    @Test
    void acceptsPcAndSeparateItems() {
        assertDoesNotThrow(() -> OrderServiceImpl.validatePcGroups(List.of(pc), Map.of(1, 1, 2, 2, 3, 1)));
    }

    @Test
    void rejectsMissingOverlappingOrMixedQuantities() {
        assertThrows(BusinessException.class, () -> OrderServiceImpl.validatePcGroups(List.of(pc), Map.of(1, 1)));
        assertThrows(BusinessException.class, () -> OrderServiceImpl.validatePcGroups(List.of(pc, pc), Map.of(1, 1, 2, 2)));
        assertThrows(BusinessException.class, () -> OrderServiceImpl.validatePcGroups(List.of(pc), Map.of(1, 2, 2, 2)));
    }
}
