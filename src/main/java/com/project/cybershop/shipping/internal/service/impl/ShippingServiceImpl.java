package com.project.cybershop.shipping.internal.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import com.project.cybershop.shipping.api.dto.ShippingDtos.*;
import com.project.cybershop.shipping.internal.client.GhnClient;
import com.project.cybershop.shipping.internal.config.GhnConfig;
import com.project.cybershop.shipping.internal.entity.Shipment;
import com.project.cybershop.shipping.internal.repository.ShipmentRepository;
import com.project.cybershop.shipping.internal.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceImpl implements ShippingService {

    private final GhnClient ghnClient;
    private final GhnConfig ghnConfig;
    private final ShipmentRepository shipmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // In-memory caches for Master Data
    private final List<ProvinceDto> cachedProvinces = new ArrayList<>();
    private final Map<Integer, List<DistrictDto>> cachedDistricts = new ConcurrentHashMap<>();
    private final Map<Integer, List<WardDto>> cachedWards = new ConcurrentHashMap<>();

    public record OrderRecord(
            Integer id, String orderCode, Integer userId, String status,
            BigDecimal totalAmount, BigDecimal finalAmount, String pickupName,
            String pickupPhone, String note, String deliveryType, BigDecimal shippingFee,
            String shippingAddress, Integer toProvinceId, Integer toDistrictId,
            String toWardCode, String trackingCode, String paymentStatus, String methodType
    ) {}

    @Override
    public List<ProvinceDto> getProvinces() {
        if (!cachedProvinces.isEmpty()) {
            return cachedProvinces;
        }
        try {
            JsonNode data = ghnClient.getProvinces();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    cachedProvinces.add(new ProvinceDto(
                            item.path("ProvinceID").asInt(),
                            item.path("ProvinceName").asText()
                    ));
                }
                cachedProvinces.sort(Comparator.comparing(ProvinceDto::name));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch provinces from GHN, using fallback: {}", e.getMessage());
            return getFallbackProvinces();
        }
        return cachedProvinces;
    }

    @Override
    public List<DistrictDto> getDistricts(int provinceId) {
        if (cachedDistricts.containsKey(provinceId)) {
            return cachedDistricts.get(provinceId);
        }
        List<DistrictDto> list = new ArrayList<>();
        try {
            JsonNode data = ghnClient.getDistricts(provinceId);
            if (data.isArray()) {
                for (JsonNode item : data) {
                    list.add(new DistrictDto(
                            item.path("DistrictID").asInt(),
                            item.path("ProvinceID").asInt(),
                            item.path("DistrictName").asText()
                    ));
                }
                list.sort(Comparator.comparing(DistrictDto::name));
                cachedDistricts.put(provinceId, list);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch districts for province {} from GHN: {}", provinceId, e.getMessage());
            return getFallbackDistricts(provinceId);
        }
        return list;
    }

    @Override
    public List<WardDto> getWards(int districtId) {
        if (cachedWards.containsKey(districtId)) {
            return cachedWards.get(districtId);
        }
        List<WardDto> list = new ArrayList<>();
        try {
            JsonNode data = ghnClient.getWards(districtId);
            if (data.isArray()) {
                for (JsonNode item : data) {
                    list.add(new WardDto(
                            item.path("WardCode").asText(),
                            item.path("DistrictID").asInt(),
                            item.path("WardName").asText()
                    ));
                }
                list.sort(Comparator.comparing(WardDto::name));
                cachedWards.put(districtId, list);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch wards for district {} from GHN: {}", districtId, e.getMessage());
            return getFallbackWards(districtId);
        }
        return list;
    }

    @Override
    public CalculateFeeResponse calculateFee(CalculateFeeRequest request) {
        if (request.toDistrictId() == null || request.toWardCode() == null || request.toWardCode().isBlank()) {
            return new CalculateFeeResponse(BigDecimal.valueOf(25000), "Giao hàng chuẩn GHN");
        }
        try {
            int weight = request.weightGram() != null && request.weightGram() > 0 ? request.weightGram() : 1000;
            JsonNode data = ghnClient.calculateFee(request.toDistrictId(), request.toWardCode(), weight);
            double total = data.path("total").asDouble(25000.0);
            return new CalculateFeeResponse(BigDecimal.valueOf(total), "Giao hàng chuẩn GHN");
        } catch (Exception e) {
            log.warn("GHN calculate fee failed, using standard fallback fee: {}", e.getMessage());
            return new CalculateFeeResponse(BigDecimal.valueOf(25000), "Giao hàng chuẩn GHN (Ước tính)");
        }
    }

    @Override
    @Transactional
    public TrackingResponse createShipment(String orderCode, String actorEmail) {
        OrderRecord order = findOrder(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode);
        }

        // Check if shipment already exists
        Optional<Shipment> existing = shipmentRepository.findByOrderId(order.id());
        if (existing.isPresent()) {
            jdbcTemplate.update(
                    "UPDATE orders SET status = 'ready_for_pickup', tracking_code = ?, updated_at = now() WHERE id = ? AND status = 'preparing'",
                    existing.get().getTrackingCode(), order.id()
            );
            return buildTrackingResponse(order, existing.get());
        }

        BigDecimal codAmount = BigDecimal.ZERO;
        // If paid by VNPAY, COD = 0. If payment is not SUCCESS, COD is the finalAmount
        if (!"SUCCESS".equalsIgnoreCase(order.paymentStatus())) {
            codAmount = order.finalAmount();
        }

        // Prepare GHN payload
        Map<String, Object> ghnPayload = new LinkedHashMap<>();
        ghnPayload.put("payment_type_id", 1); // 1 = Shop pays shipping fee
        ghnPayload.put("note", order.note() != null && !order.note().isBlank() ? order.note() : "Đơn hàng từ CyberShop");
        ghnPayload.put("required_note", "CHOXEMHANGKHONGTHU");
        ghnPayload.put("to_name", order.pickupName() != null ? order.pickupName() : "Khách hàng");
        ghnPayload.put("to_phone", order.pickupPhone() != null ? order.pickupPhone() : "0900000000");
        ghnPayload.put("to_address", order.shippingAddress() != null && !order.shippingAddress().isBlank()
                ? order.shippingAddress() : "Địa chỉ nhận hàng");
        ghnPayload.put("to_district_id", order.toDistrictId() != null ? order.toDistrictId() : 1442);
        ghnPayload.put("to_ward_code", order.toWardCode() != null ? order.toWardCode() : "20101");
        ghnPayload.put("cod_amount", codAmount.intValue());
        ghnPayload.put("weight", 1000);
        ghnPayload.put("length", 20);
        ghnPayload.put("width", 20);
        ghnPayload.put("height", 10);
        ghnPayload.put("service_type_id", 2);

        // Sender / Return info from config
        ghnPayload.put("from_name", ghnConfig.getFromName());
        ghnPayload.put("from_phone", ghnConfig.getFromPhone());
        ghnPayload.put("from_address", ghnConfig.getFromAddress());
        if (ghnConfig.getFromDistrictId() != null) {
            ghnPayload.put("return_district_id", ghnConfig.getFromDistrictId());
        }
        if (ghnConfig.getFromWardCode() != null) {
            ghnPayload.put("return_ward_code", ghnConfig.getFromWardCode());
        }
        ghnPayload.put("return_phone", ghnConfig.getFromPhone());
        ghnPayload.put("return_address", ghnConfig.getFromAddress());

        // Fetch order items to list in GHN payload
        List<Map<String, Object>> itemsList = new ArrayList<>();
        List<Map<String, Object>> dbItems = jdbcTemplate.queryForList(
                "SELECT product_name, quantity, unit_price FROM order_items WHERE order_id = ?",
                order.id()
        );
        for (Map<String, Object> r : dbItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("name", r.get("product_name"));
            itemMap.put("quantity", ((Number) r.get("quantity")).intValue());
            itemMap.put("price", ((Number) r.get("unit_price")).intValue());
            itemMap.put("weight", 500);
            itemsList.add(itemMap);
        }
        if (itemsList.isEmpty()) {
            itemsList.add(Map.of("name", "Linh kiện máy tính", "quantity", 1, "price", order.finalAmount().intValue(), "weight", 1000));
        }
        ghnPayload.put("items", itemsList);

        String trackingCode = null;
        BigDecimal actualShippingFee = order.shippingFee() != null ? order.shippingFee() : BigDecimal.valueOf(25000);
        LocalDateTime expectedDelivery = LocalDateTime.now().plusDays(2);

        try {
            JsonNode ghnRes = ghnClient.createShippingOrder(ghnPayload);
            trackingCode = ghnRes.path("order_code").asText();
            if (trackingCode == null || trackingCode.isBlank()) {
                throw new BusinessException("GHN không trả về mã vận đơn.");
            }
            if (ghnRes.has("total_fee")) {
                actualShippingFee = BigDecimal.valueOf(ghnRes.path("total_fee").asDouble());
            }
            if (ghnRes.has("expected_delivery_time")) {
                String expStr = ghnRes.path("expected_delivery_time").asText();
                try {
                    expectedDelivery = LocalDateTime.parse(expStr, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception ignored) {}
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("GHN create order call failed: {}", e.getMessage(), e);
            throw new BusinessException("Không thể tạo vận đơn trên GHN: " + e.getMessage());
        }

        // Generate Print Token
        String printToken = null;
        try {
            JsonNode printRes = ghnClient.generatePrintToken(trackingCode);
            printToken = printRes.path("token").asText(null);
        } catch (Exception ignored) {}

        // Save Shipment
        Shipment shipment = new Shipment();
        shipment.setOrderId(order.id());
        shipment.setCarrier("GHN");
        shipment.setTrackingCode(trackingCode);
        shipment.setCodAmount(codAmount);
        shipment.setShippingFee(actualShippingFee);
        shipment.setStatus("ready_to_pick");
        shipment.setStatusText("Chờ lấy hàng");
        shipment.setExpectedDeliveryTime(expectedDelivery);
        shipment.setPrintToken(printToken);
        shipment.setLastSyncedAt(LocalDateTime.now());

        // Initial log
        List<TrackingLogItem> logs = new ArrayList<>();
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
        logs.add(new TrackingLogItem(nowStr, "ready_to_pick", "Đã tạo vận đơn GHN thành công. Đang chờ shipper đến lấy hàng.", "Kho CyberShop (Quận 12, TP.HCM)"));
        try {
            shipment.setTrackingLogs(objectMapper.writeValueAsString(logs));
        } catch (Exception ignored) {}

        shipmentRepository.save(shipment);

        // Update Order to ready_for_pickup with real GHN tracking code
        jdbcTemplate.update(
                "UPDATE orders SET tracking_code = ?, carrier = 'GHN', status = 'ready_for_pickup', updated_at = now() WHERE id = ?",
                trackingCode, order.id()
        );

        return buildTrackingResponse(order, shipment);
    }

    @Override
    @Transactional
    public TrackingResponse getTracking(String orderCode, String actorEmail) {
        OrderRecord order = findOrder(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode);
        }

        Optional<Shipment> opt = shipmentRepository.findByOrderId(order.id());
        if (opt.isEmpty()) {
            // Shipment not created yet
            return new TrackingResponse(
                    order.orderCode(), null, "GHN", order.status(),
                    "Đang xử lý tại cửa hàng", null, null,
                    BigDecimal.ZERO, order.shippingFee(), Collections.emptyList()
            );
        }

        Shipment shipment = opt.get();

        // Smart Cache: Check if we should sync from GHN
        // If not in final/simulated state and synced > 60 seconds ago
        boolean isFinalOrSimulated = Set.of("delivered", "cancel", "completed", "delivering")
                .contains(shipment.getStatus().toLowerCase());
        boolean shouldSync = !isFinalOrSimulated && (shipment.getLastSyncedAt() == null ||
                shipment.getLastSyncedAt().isBefore(LocalDateTime.now().minusSeconds(60)));

        if (shouldSync) {
            try {
                JsonNode detail = ghnClient.getOrderDetail(shipment.getTrackingCode());
                if (detail != null && detail.isObject()) {
                    String ghnStatus = detail.path("status").asText(shipment.getStatus());
                    String statusName = detail.path("status_name").asText(translateStatus(ghnStatus));

                    shipment.setStatus(ghnStatus);
                    shipment.setStatusText(statusName);
                    shipment.setLastSyncedAt(LocalDateTime.now());

                    // If delivered, mark completed
                    if ("delivered".equalsIgnoreCase(ghnStatus)) {
                        jdbcTemplate.update("UPDATE orders SET status = 'completed', updated_at = now() WHERE id = ?", order.id());
                        if (!"SUCCESS".equalsIgnoreCase(order.paymentStatus())) {
                            jdbcTemplate.update("UPDATE payments SET status = 'SUCCESS', paid_at = now() WHERE order_id = ?", order.id());
                        }
                    }

                    // Extract logs if present
                    if (detail.has("log") && detail.path("log").isArray()) {
                        List<TrackingLogItem> ghnLogs = new ArrayList<>();
                        for (JsonNode l : detail.path("log")) {
                            ghnLogs.add(new TrackingLogItem(
                                    l.path("updated_date").asText(""),
                                    l.path("status").asText(""),
                                    translateStatus(l.path("status").asText("")),
                                    l.path("location").asText("Bưu cục GHN")
                            ));
                        }
                        if (!ghnLogs.isEmpty()) {
                            shipment.setTrackingLogs(objectMapper.writeValueAsString(ghnLogs));
                        }
                    }
                    shipmentRepository.save(shipment);
                }
            } catch (Exception e) {
                log.info("Could not sync GHN status for {}: {}", shipment.getTrackingCode(), e.getMessage());
            }
        }

        return buildTrackingResponse(order, shipment);
    }

    @Override
    @Transactional
    public TrackingResponse switchStatus(String orderCode, String newStatus, String actorEmail) {
        OrderRecord order = findOrder(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode);
        }

        Shipment shipment = shipmentRepository.findByOrderId(order.id())
                .orElseThrow(() -> new BusinessException("Đơn hàng chưa được đẩy sang GHN để đổi trạng thái."));

        String statusText = translateStatus(newStatus);
        shipment.setStatus(newStatus);
        shipment.setStatusText(statusText);
        shipment.setLastSyncedAt(LocalDateTime.now());

        // Append to logs
        List<TrackingLogItem> currentLogs = parseLogs(shipment.getTrackingLogs());
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"));
        currentLogs.add(0, new TrackingLogItem(nowStr, newStatus, statusText, "Hệ thống GHN"));

        try {
            shipment.setTrackingLogs(objectMapper.writeValueAsString(currentLogs));
        } catch (Exception ignored) {}

        // Update database order / payment
        if ("delivering".equalsIgnoreCase(newStatus)) {
            jdbcTemplate.update("UPDATE orders SET status = 'delivering', updated_at = now() WHERE id = ?", order.id());
        } else if ("delivered".equalsIgnoreCase(newStatus)) {
            jdbcTemplate.update("UPDATE orders SET status = 'completed', updated_at = now() WHERE id = ?", order.id());
            if (!"SUCCESS".equalsIgnoreCase(order.paymentStatus())) {
                jdbcTemplate.update("UPDATE payments SET status = 'SUCCESS', paid_at = now() WHERE order_id = ?", order.id());
            }
        } else if ("cancel".equalsIgnoreCase(newStatus)) {
            jdbcTemplate.update("UPDATE orders SET status = 'canceled', updated_at = now() WHERE id = ?", order.id());
        }

        shipmentRepository.save(shipment);

        // Also attempt to notify GHN if cancel
        if ("cancel".equalsIgnoreCase(newStatus)) {
            try {
                ghnClient.switchStatus("cancel", shipment.getTrackingCode());
            } catch (Exception ignored) {}
        }

        return buildTrackingResponse(order, shipment);
    }

    @Override
    public String getPrintToken(String orderCode) {
        OrderRecord order = findOrder(orderCode);
        if (order == null) throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode);

        Shipment shipment = shipmentRepository.findByOrderId(order.id())
                .orElseThrow(() -> new BusinessException("Đơn hàng chưa có mã vận đơn GHN."));

        if (shipment.getPrintToken() != null && !shipment.getPrintToken().isBlank()) {
            return shipment.getPrintToken();
        }

        try {
            JsonNode printRes = ghnClient.generatePrintToken(shipment.getTrackingCode());
            String token = printRes.path("token").asText(null);
            if (token != null) {
                shipment.setPrintToken(token);
                shipmentRepository.save(shipment);
                return token;
            }
        } catch (Exception e) {
            log.warn("Failed to generate GHN print token: {}", e.getMessage());
        }
        return "mock_print_token_" + shipment.getTrackingCode();
    }

    @Override
    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("OrderCode")) return;

        String ghnOrderCode = String.valueOf(payload.get("OrderCode"));
        String status = String.valueOf(payload.getOrDefault("Status", ""));

        Optional<Shipment> opt = shipmentRepository.findByTrackingCode(ghnOrderCode);
        if (opt.isEmpty()) return;

        Shipment shipment = opt.get();
        shipment.setStatus(status);
        shipment.setStatusText(translateStatus(status));
        shipment.setLastSyncedAt(LocalDateTime.now());

        if ("delivered".equalsIgnoreCase(status)) {
            jdbcTemplate.update("UPDATE orders SET status = 'completed', updated_at = now() WHERE id = ?", shipment.getOrderId());
            jdbcTemplate.update("UPDATE payments SET status = 'SUCCESS', paid_at = now() WHERE order_id = ?", shipment.getOrderId());
        } else if ("delivering".equalsIgnoreCase(status)) {
            jdbcTemplate.update("UPDATE orders SET status = 'delivering', updated_at = now() WHERE id = ?", shipment.getOrderId());
        }

        shipmentRepository.save(shipment);
    }

    private TrackingResponse buildTrackingResponse(OrderRecord order, Shipment shipment) {
        String printUrl = shipment.getPrintToken() != null
                ? "https://dev-online-gateway.ghn.vn/a5/index.html?token=" + shipment.getPrintToken()
                : null;

        String expStr = shipment.getExpectedDeliveryTime() != null
                ? shipment.getExpectedDeliveryTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "2 - 3 ngày";

        List<TrackingLogItem> logs = parseLogs(shipment.getTrackingLogs());

        return new TrackingResponse(
                order.orderCode(),
                shipment.getTrackingCode(),
                shipment.getCarrier(),
                shipment.getStatus(),
                shipment.getStatusText(),
                expStr,
                printUrl,
                shipment.getCodAmount(),
                shipment.getShippingFee(),
                logs
        );
    }

    private List<TrackingLogItem> parseLogs(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<TrackingLogItem>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private OrderRecord findOrder(String orderCode) {
        List<OrderRecord> list = jdbcTemplate.query(
                "SELECT o.id, o.order_code, o.user_id, o.status, o.total_amount, o.final_amount, " +
                "o.pickup_name, o.pickup_phone, o.note, o.delivery_type, o.shipping_fee, " +
                "o.shipping_address, o.to_province_id, o.to_district_id, o.to_ward_code, o.tracking_code, " +
                "p.status as payment_status, p.method_type " +
                "FROM orders o LEFT JOIN payments p ON o.id = p.order_id " +
                "WHERE o.order_code = ?",
                (rs, rowNum) -> new OrderRecord(
                        rs.getInt("id"),
                        rs.getString("order_code"),
                        rs.getInt("user_id"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("final_amount"),
                        rs.getString("pickup_name"),
                        rs.getString("pickup_phone"),
                        rs.getString("note"),
                        rs.getString("delivery_type"),
                        rs.getBigDecimal("shipping_fee"),
                        rs.getString("shipping_address"),
                        (Integer) rs.getObject("to_province_id"),
                        (Integer) rs.getObject("to_district_id"),
                        rs.getString("to_ward_code"),
                        rs.getString("tracking_code"),
                        rs.getString("payment_status"),
                        rs.getString("method_type")
                ),
                orderCode
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private String translateStatus(String status) {
        if (status == null) return "Đang xử lý";
        return switch (status.toLowerCase()) {
            case "ready_to_pick" -> "Chờ shipper lấy hàng";
            case "picking" -> "Shipper đang lấy hàng";
            case "picked" -> "Shipper đã nhận hàng từ shop";
            case "storing" -> "Hàng đang lưu kho trung chuyển";
            case "transporting" -> "Đang luân chuyển liên tỉnh";
            case "delivering" -> "Đang giao hàng tới người nhận";
            case "delivered" -> "Đã giao hàng thành công";
            case "delivery_fail" -> "Giao hàng không thành công";
            case "cancel" -> "Đơn vận chuyển đã hủy";
            default -> "Đang xử lý: " + status;
        };
    }

    private List<ProvinceDto> getFallbackProvinces() {
        return List.of(
                new ProvinceDto(201, "Hà Nội"),
                new ProvinceDto(202, "Hồ Chí Minh"),
                new ProvinceDto(203, "Đà Nẵng"),
                new ProvinceDto(204, "Cần Thơ"),
                new ProvinceDto(205, "Hải Phòng"),
                new ProvinceDto(206, "Bình Dương"),
                new ProvinceDto(207, "Đồng Nai")
        );
    }

    private List<DistrictDto> getFallbackDistricts(int provinceId) {
        if (provinceId == 202) {
            return List.of(
                    new DistrictDto(1442, 202, "Quận 1"),
                    new DistrictDto(1443, 202, "Quận 3"),
                    new DistrictDto(1444, 202, "Quận 10"),
                    new DistrictDto(1454, 202, "Quận 12"),
                    new DistrictDto(1450, 202, "Quận Bình Thạnh"),
                    new DistrictDto(1451, 202, "Quận Tân Bình"),
                    new DistrictDto(1452, 202, "TP. Thủ Đức")
            );
        }
        return List.of(new DistrictDto(1442, provinceId, "Quận Trung Tâm"));
    }

    private List<WardDto> getFallbackWards(int districtId) {
        return List.of(
                new WardDto("20101", districtId, "Phường Bến Nghé"),
                new WardDto("20102", districtId, "Phường Bến Thành"),
                new WardDto("21210", districtId, "Phường Thới An"),
                new WardDto("21211", districtId, "Phường Hiệp Thành")
        );
    }
}
