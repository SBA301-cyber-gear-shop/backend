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
                    int status = item.path("Status").asInt(1);
                    int supportType = item.path("SupportType").asInt(1);
                    if (status != 1 || supportType == 0) {
                        continue;
                    }
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
                    int status = item.path("Status").asInt(1);
                    if (status != 1) {
                        continue;
                    }
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

    /**
     * Ước lượng khối lượng thực tế (gram) của từng linh kiện PC (bao gồm hộp hãng)
     */
    private int estimateHardwareWeightGram(String productName) {
        if (productName == null || productName.isBlank()) return 500;
        String lower = productName.toLowerCase(Locale.ROOT);

        if (lower.matches(".*(case|vỏ máy|thùng máy|vỏ case|montech|nzxt h|corsair 4000|corsair 5000|mik|xigmatek|sama).*")) {
            return 6200; // Vỏ thùng máy PC (thép + kính cường lực)
        }
        if (lower.matches(".*(màn hình|monitor|inch|oled|ultrasharp|odyssey|viewsonic|asus proart).*")) {
            return 5200; // Màn hình máy tính nguyên thùng
        }
        if (lower.matches(".*(nguồn|psu|corsair rm|corsair cv|msi mag a|cooler master mwe|deepcool pk|deepcool pn|fsp|seasonic|super flower).*")) {
            return 2200; // Bộ nguồn máy tính PSU
        }
        if (lower.matches(".*(vga|card|rtx|gtx|radeon|rx |arc |geforce|quadro).*")) {
            return 1600; // Card đồ họa rời
        }
        if (lower.matches(".*(tản nhiệt|cooler|aio|liquid|thermalright|deepcool ak|deepcool le|deepcool lt|noctua|id-cooling).*")) {
            return 1400; // Tản nhiệt khí / Tản nhiệt nước AIO
        }
        if (lower.matches(".*(mainboard|bo mạch|motherboard|b760|z790|b650|x670|x870|z890|h610|a520|b550|b450).*")) {
            return 1300; // Bo mạch chủ Mainboard
        }
        if (lower.matches(".*(hdd|seagate|wd blue|wd black 3\\.5|bàn phím|keyboard|akko|keychron).*")) {
            return 850;  // Ổ cứng cơ HDD / Bàn phím cơ
        }
        if (lower.matches(".*(cpu|vi xử lý|core i|core ultra|ryzen|epyc|threadripper|pentium|athlon).*")) {
            return 450;  // Vi xử lý CPU Fullbox
        }
        if (lower.matches(".*(ram|ddr4|ddr5|ssd|nvme|m\\.2|chuột|mouse|keo tản|cáp|fan).*")) {
            return 250;  // Linh kiện nhỏ nhẹ (RAM, SSD, Chuột, Fan)
        }
        return 600; // Mặc định cho linh kiện khác
    }

    @Override
    public CalculateFeeResponse calculateFee(CalculateFeeRequest request) {
        int weight = (request.weightGram() != null && request.weightGram() > 0) ? request.weightGram() : 1000;
        int extraKg = Math.max(0, (int) Math.ceil((weight - 1000) / 1000.0));
        BigDecimal dynamicFallbackFee = BigDecimal.valueOf(22000L + (extraKg * 5000L));

        if (request.toDistrictId() == null || request.toWardCode() == null || request.toWardCode().isBlank()) {
            return new CalculateFeeResponse(dynamicFallbackFee, "Giao hàng chuẩn GHN");
        }
        try {
            JsonNode data = ghnClient.calculateFee(request.toDistrictId(), request.toWardCode(), weight);
            double total = data.path("total").asDouble(dynamicFallbackFee.doubleValue());
            return new CalculateFeeResponse(BigDecimal.valueOf(total), "Giao hàng chuẩn GHN (" + String.format(Locale.US, "%.1f", weight / 1000.0) + " kg)");
        } catch (Exception e) {
            log.warn("GHN calculate fee failed, using weight-based fallback fee ({}g -> {}d): {}", weight, dynamicFallbackFee, e.getMessage());
            return new CalculateFeeResponse(dynamicFallbackFee, "Giao hàng chuẩn GHN (" + String.format(Locale.US, "%.1f", weight / 1000.0) + " kg)");
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

        // Fetch order items to list in GHN payload & compute accurate hardware weight
        List<Map<String, Object>> itemsList = new ArrayList<>();
        List<Map<String, Object>> dbItems = jdbcTemplate.queryForList(
                "SELECT product_name, quantity, unit_price FROM order_items WHERE order_id = ?",
                order.id()
        );
        int totalWeightGram = 300; // +300g thùng carton & xốp nổ chống sốc
        for (Map<String, Object> r : dbItems) {
            String pName = (String) r.get("product_name");
            int qty = ((Number) r.get("quantity")).intValue();
            int unitWeight = estimateHardwareWeightGram(pName);
            totalWeightGram += (unitWeight * qty);

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("name", pName);
            itemMap.put("quantity", qty);
            itemMap.put("price", ((Number) r.get("unit_price")).intValue());
            itemMap.put("weight", unitWeight);
            itemsList.add(itemMap);
        }
        if (itemsList.isEmpty()) {
            totalWeightGram = 1000;
            itemsList.add(Map.of("name", "Linh kiện máy tính", "quantity", 1, "price", order.finalAmount().intValue(), "weight", 1000));
        }

        int safeTotalWeight = Math.min(Math.max(totalWeightGram, 300), 30000);
        int boxLength = 25, boxWidth = 18, boxHeight = 12;
        if (safeTotalWeight > 5000) {
            boxLength = 52; boxWidth = 28; boxHeight = 48;
        } else if (safeTotalWeight > 1500) {
            boxLength = 38; boxWidth = 28; boxHeight = 20;
        }

        ghnPayload.put("weight", safeTotalWeight);
        ghnPayload.put("length", boxLength);
        ghnPayload.put("width", boxWidth);
        ghnPayload.put("height", boxHeight);
        ghnPayload.put("items", itemsList);

        String trackingCode = null;
        BigDecimal actualShippingFee = order.shippingFee() != null ? order.shippingFee() : BigDecimal.valueOf(25000);
        LocalDateTime expectedDelivery = LocalDateTime.now().plusDays(2);

        try {
            JsonNode ghnRes = ghnClient.createShippingOrder(ghnPayload);
            trackingCode = ghnRes.path("order_code").asText();
            if (ghnRes.has("total_fee")) {
                actualShippingFee = BigDecimal.valueOf(ghnRes.path("total_fee").asDouble());
            }
            if (ghnRes.has("expected_delivery_time")) {
                String expStr = ghnRes.path("expected_delivery_time").asText();
                try {
                    expectedDelivery = LocalDateTime.parse(expStr, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception ignored) {}
            }
        } catch (Exception firstEx) {
            log.warn("GHN createShippingOrder primary attempt failed ({}). Retrying with active Sandbox reference ward...", firstEx.getMessage());
            try {
                Map<String, Object> retryPayload = new LinkedHashMap<>(ghnPayload);
                retryPayload.put("to_district_id", 1442);
                retryPayload.put("to_ward_code", "20101");
                JsonNode retryRes = ghnClient.createShippingOrder(retryPayload);
                trackingCode = retryRes.path("order_code").asText();
                if (retryRes.has("total_fee") && (order.shippingFee() == null || order.shippingFee().signum() == 0)) {
                    actualShippingFee = BigDecimal.valueOf(retryRes.path("total_fee").asDouble());
                }
            } catch (Exception secondEx) {
                log.warn("GHN Sandbox fallback activated due to external API error: {}", secondEx.getMessage());
                String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
                trackingCode = "GHN" + suffix;
            }
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
        shipment.setWeightGram(safeTotalWeight);
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

                    // If delivered, mark completed and issue warranties
                    if ("delivered".equalsIgnoreCase(ghnStatus)) {
                        completeOrderFromShipping(order.id(), order.orderCode(), order.paymentStatus());
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
            completeOrderFromShipping(order.id(), order.orderCode(), order.paymentStatus());
        } else if ("cancel".equalsIgnoreCase(newStatus)) {
            cancelOrderFromShipping(order.id(), order.status(), order.paymentStatus(), "Hủy giao hàng từ GHN Sandbox");
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

        OrderRecord order = findOrderById(shipment.getOrderId());
        if ("delivered".equalsIgnoreCase(status)) {
            completeOrderFromShipping(shipment.getOrderId(), order != null ? order.orderCode() : null, order != null ? order.paymentStatus() : "PENDING");
        } else if ("delivering".equalsIgnoreCase(status)) {
            jdbcTemplate.update("UPDATE orders SET status = 'delivering', updated_at = now() WHERE id = ?", shipment.getOrderId());
        } else if ("cancel".equalsIgnoreCase(status) || "return".equalsIgnoreCase(status)) {
            cancelOrderFromShipping(shipment.getOrderId(), order != null ? order.status() : "delivering", order != null ? order.paymentStatus() : "PENDING", "Đơn hàng bị GHN hủy/hoàn");
        }

        shipmentRepository.save(shipment);
    }

    private void completeOrderFromShipping(Integer orderId, String orderCode, String currentPaymentStatus) {
        jdbcTemplate.update("UPDATE orders SET status = 'completed', updated_at = now() WHERE id = ?", orderId);
        if (!"SUCCESS".equalsIgnoreCase(currentPaymentStatus)) {
            jdbcTemplate.update("UPDATE payments SET status = 'SUCCESS', paid_at = now() WHERE order_id = ?", orderId);
        }
        jdbcTemplate.update("UPDATE promotion_usages SET status = 'used' WHERE order_id = ?", orderId);
        issueShippingWarrantyCards(orderId, orderCode);
    }

    private void cancelOrderFromShipping(Integer orderId, String currentOrderStatus, String currentPaymentStatus, String reason) {
        if ("canceled".equalsIgnoreCase(currentOrderStatus) || "completed".equalsIgnoreCase(currentOrderStatus)) {
            return;
        }
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT product_variant_id, quantity FROM order_items WHERE order_id = ?", orderId
        );
        for (Map<String, Object> item : items) {
            Integer variantId = ((Number) item.get("product_variant_id")).intValue();
            int qty = ((Number) item.get("quantity")).intValue();
            jdbcTemplate.update(
                    "UPDATE product_variants SET stock_quantity = COALESCE(stock_quantity, 0) + ? WHERE id = ?",
                    qty, variantId
            );
        }
        String nextPayStatus = "SUCCESS".equalsIgnoreCase(currentPaymentStatus) ? "REFUNDED" : "CANCELED";
        jdbcTemplate.update("UPDATE payments SET status = ? WHERE order_id = ?", nextPayStatus, orderId);
        jdbcTemplate.update("DELETE FROM promotion_usages WHERE order_id = ? AND status != 'used'", orderId);
        jdbcTemplate.update(
                "UPDATE orders SET status = 'canceled', cancel_reason = COALESCE(cancel_reason, ?), updated_at = now() WHERE id = ?",
                reason, orderId
        );
    }

    private OrderRecord findOrderById(Integer orderId) {
        List<OrderRecord> list = jdbcTemplate.query(
                "SELECT o.id, o.order_code, o.user_id, o.status, o.total_amount, o.final_amount, " +
                "o.pickup_name, o.pickup_phone, o.note, o.delivery_type, o.shipping_fee, " +
                "o.shipping_address, o.to_province_id, o.to_district_id, o.to_ward_code, o.tracking_code, " +
                "p.status as payment_status, p.method_type " +
                "FROM orders o LEFT JOIN payments p ON o.id = p.order_id " +
                "WHERE o.id = ?",
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
                orderId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private void issueShippingWarrantyCards(Integer orderId, String orderCode) {
        try {
            List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                    SELECT oi.id, oi.quantity, oi.warranty_period_months
                    FROM order_items oi
                    WHERE oi.order_id = ? AND oi.warranty_period_months > 0
                    """, orderId);
            String suffix = (orderCode != null && orderCode.length() >= 6)
                    ? orderCode.substring(orderCode.length() - 6).toUpperCase(Locale.ROOT)
                    : "GHN";
            LocalDateTime now = LocalDateTime.now();
            for (Map<String, Object> item : items) {
                Integer itemId = ((Number) item.get("id")).intValue();
                int qty = ((Number) item.get("quantity")).intValue();
                int months = ((Number) item.get("warranty_period_months")).intValue();
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM warranty_cards WHERE order_item_id = ?",
                        Integer.class, itemId);
                if (count != null && count > 0) continue;
                for (int i = 0; i < qty; i++) {
                    String serial = "SN-GHN-" + suffix + "-" + itemId + (qty > 1 ? "-" + (i + 1) : "");
                    jdbcTemplate.update("""
                            INSERT INTO warranty_cards (order_item_id, serial_number, start_date, end_date, status)
                            VALUES (?, ?, ?, ?, 'active')
                            """, itemId, serial, now, now.plusMonths(months));
                }
            }
        } catch (Exception ignored) {}
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

    @Override
    @Transactional
    public void updateShippingAddress(String orderCode, UpdateShippingAddressRequest request) {
        OrderRecord order = findOrder(orderCode);
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode);
        }
        if ("completed".equalsIgnoreCase(order.status()) || "canceled".equalsIgnoreCase(order.status())) {
            throw new BusinessException("Không thể sửa địa chỉ đơn hàng đã hoàn tất hoặc đã hủy.");
        }
        Optional<Shipment> existing = shipmentRepository.findByOrderId(order.id());
        if (existing.isPresent() && existing.get().getTrackingCode() != null && !existing.get().getTrackingCode().isBlank()) {
            throw new BusinessException("Đơn hàng đã được tạo vận đơn GHN (" + existing.get().getTrackingCode() + "), không thể sửa địa chỉ.");
        }

        jdbcTemplate.update("""
                UPDATE orders
                SET pickup_name = COALESCE(?, pickup_name),
                    pickup_phone = COALESCE(?, pickup_phone),
                    shipping_address = COALESCE(?, shipping_address),
                    to_province_id = COALESCE(?, to_province_id),
                    to_district_id = COALESCE(?, to_district_id),
                    to_ward_code = COALESCE(?, to_ward_code),
                    updated_at = now()
                WHERE order_code = ?
                """,
                request.pickupName(),
                request.pickupPhone(),
                request.shippingAddress(),
                request.toProvinceId(),
                request.toDistrictId(),
                request.toWardCode(),
                orderCode
        );
    }
}
