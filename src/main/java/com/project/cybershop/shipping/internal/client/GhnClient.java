package com.project.cybershop.shipping.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.shipping.internal.config.GhnConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnClient {

    private final GhnConfig ghnConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonNode getProvinces() {
        return executeGet(ghnConfig.getBaseUrl() + "/master-data/province", false);
    }

    public JsonNode getDistricts(int provinceId) {
        String body = String.format("{\"province_id\": %d}", provinceId);
        return executePost(ghnConfig.getBaseUrl() + "/master-data/district", body, false);
    }

    public JsonNode getWards(int districtId) {
        return executeGet(ghnConfig.getBaseUrl() + "/master-data/ward?district_id=" + districtId, false);
    }

    public JsonNode calculateFee(int toDistrictId, String toWardCode, int weightGram) {
        int safeWeight = Math.min(Math.max(weightGram, 300), 30000);
        int length = 25, width = 18, height = 12;
        if (safeWeight > 5000) {
            length = 52; width = 28; height = 48;
        } else if (safeWeight > 1500) {
            length = 38; width = 28; height = 20;
        }

        Map<String, Object> body = Map.of(
                "service_type_id", 2,
                "from_district_id", ghnConfig.getFromDistrictId(),
                "to_district_id", toDistrictId,
                "to_ward_code", toWardCode,
                "height", height,
                "length", length,
                "width", width,
                "weight", safeWeight
        );
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return executePost(ghnConfig.getBaseUrl() + "/v2/shipping-order/fee", jsonBody, true);
        } catch (Exception e) {
            log.error("Failed to serialize fee request", e);
            throw new RuntimeException("Lỗi tính phí vận chuyển GHN: " + e.getMessage(), e);
        }
    }

    public JsonNode createShippingOrder(Map<String, Object> payload) {
        try {
            String jsonBody = objectMapper.writeValueAsString(payload);
            return executePost(ghnConfig.getBaseUrl() + "/v2/shipping-order/create", jsonBody, true);
        } catch (Exception e) {
            log.error("Failed to create GHN shipping order", e);
            throw new RuntimeException("Lỗi tạo vận đơn GHN: " + e.getMessage(), e);
        }
    }

    public JsonNode previewShippingOrder(Map<String, Object> payload) {
        try {
            String jsonBody = objectMapper.writeValueAsString(payload);
            return executePost(ghnConfig.getBaseUrl() + "/v2/shipping-order/preview", jsonBody, true);
        } catch (Exception e) {
            log.error("Failed to preview GHN shipping order", e);
            throw new RuntimeException("Lỗi xem trước vận đơn GHN: " + e.getMessage(), e);
        }
    }

    public JsonNode getOrderDetail(String orderCode) {
        String body = String.format("{\"order_code\": \"%s\"}", orderCode);
        return executePost(ghnConfig.getBaseUrl() + "/v2/shipping-order/detail", body, true);
    }

    public JsonNode generatePrintToken(String orderCode) {
        String body = String.format("{\"order_codes\": [\"%s\"]}", orderCode);
        return executePost(ghnConfig.getBaseUrl() + "/v2/a5/gen-token", body, false);
    }

    public JsonNode switchStatus(String action, String orderCode) {
        String body = String.format("{\"order_codes\": [\"%s\"]}", orderCode);
        return executePost(ghnConfig.getBaseUrl() + "/v2/switch-status/" + action, body, true);
    }

    private JsonNode executeGet(String url, boolean includeShopId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Token", ghnConfig.getToken())
                    .header("Content-Type", "application/json")
                    .GET();
            if (includeShopId && ghnConfig.getShopId() != null) {
                builder.header("ShopId", String.valueOf(ghnConfig.getShopId()));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return parseResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GHN request interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi kết nối GHN: " + e.getMessage(), e);
        }
    }

    private JsonNode executePost(String url, String jsonBody, boolean includeShopId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Token", ghnConfig.getToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            if (includeShopId && ghnConfig.getShopId() != null) {
                builder.header("ShopId", String.valueOf(ghnConfig.getShopId()));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return parseResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GHN request interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi kết nối GHN: " + e.getMessage(), e);
        }
    }

    private JsonNode parseResponse(HttpResponse<String> response) throws IOException {
        String body = response.body();
        JsonNode root = objectMapper.readTree(body);
        int statusCode = response.statusCode();
        int code = root.path("code").asInt(statusCode);

        if (statusCode >= 200 && statusCode < 300 && (code == 200 || code == 0)) {
            return root.path("data");
        }
        String errorMsg = root.path("message").asText(root.path("code_message_value").asText("Lỗi máy chủ GHN"));
        log.warn("GHN API returned error (HTTP {} / code {}): {}", statusCode, code, body);
        throw new RuntimeException("GHN lỗi: " + errorMsg);
    }
}
