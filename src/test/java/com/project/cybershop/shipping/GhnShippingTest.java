package com.project.cybershop.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.cybershop.shipping.api.dto.ShippingDtos.*;
import com.project.cybershop.shipping.internal.client.GhnClient;
import com.project.cybershop.shipping.internal.config.GhnConfig;
import com.project.cybershop.shipping.internal.entity.Shipment;
import com.project.cybershop.shipping.internal.repository.ShipmentRepository;
import com.project.cybershop.shipping.internal.service.impl.ShippingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GhnShippingTest {

    private GhnClient ghnClient;
    private GhnConfig ghnConfig;
    private ShipmentRepository shipmentRepository;
    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private ShippingServiceImpl shippingService;

    @BeforeEach
    void setUp() {
        ghnClient = mock(GhnClient.class);
        ghnConfig = new GhnConfig();
        shipmentRepository = mock(ShipmentRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();

        shippingService = new ShippingServiceImpl(
                ghnClient, ghnConfig, shipmentRepository, jdbcTemplate, objectMapper
        );
    }

    @Test
    void testGetProvincesSuccess() {
        ArrayNode arr = objectMapper.createArrayNode();
        ObjectNode item = arr.addObject();
        item.put("ProvinceID", 202);
        item.put("ProvinceName", "Hồ Chí Minh");

        when(ghnClient.getProvinces()).thenReturn(arr);

        List<ProvinceDto> provinces = shippingService.getProvinces();
        assertNotNull(provinces);
        assertFalse(provinces.isEmpty());
        assertEquals("Hồ Chí Minh", provinces.get(0).name());
    }

    @Test
    void testCalculateFeeSuccess() {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("total", 21001.0);

        when(ghnClient.calculateFee(eq(1442), eq("20101"), anyInt())).thenReturn(data);

        CalculateFeeResponse res = shippingService.calculateFee(new CalculateFeeRequest(1442, "20101", 1000));
        assertNotNull(res);
        assertEquals(BigDecimal.valueOf(21001.0), res.totalFee());
    }

    @Test
    void testGetTrackingWithSmartCache() {
        Shipment shipment = new Shipment();
        shipment.setId(1);
        shipment.setOrderId(10);
        shipment.setTrackingCode("G8TEST123");
        shipment.setStatus("delivering");
        shipment.setStatusText("Đang giao hàng");
        shipment.setLastSyncedAt(LocalDateTime.now().minusSeconds(10)); // within 60s cache
        shipment.setCodAmount(BigDecimal.ZERO);
        shipment.setShippingFee(BigDecimal.valueOf(21000));

        ShippingServiceImpl.OrderRecord orderRecord = new ShippingServiceImpl.OrderRecord(
                10, "TZ-TEST-01", 1, "ready_for_pickup",
                BigDecimal.valueOf(100000), BigDecimal.valueOf(100000), "Nguyen Van A",
                "0987654321", "Giao gio hanh chinh", "shipping", BigDecimal.valueOf(21000),
                "123 Nguyen Hue", 202, 1442, "20101", "G8TEST123", "SUCCESS", "vnpay"
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("TZ-TEST-01")))
                .thenReturn(List.of(orderRecord));

        when(shipmentRepository.findByOrderId(anyInt())).thenReturn(Optional.of(shipment));

        TrackingResponse tracking = shippingService.getTracking("TZ-TEST-01", "test@techzone.vn");
        assertNotNull(tracking);
        assertEquals("G8TEST123", tracking.trackingCode());
        assertEquals("delivering", tracking.status());

        // Should NOT call ghnClient.getOrderDetail because lastSyncedAt is recent (< 60s)
        verify(ghnClient, never()).getOrderDetail(anyString());
    }
}
