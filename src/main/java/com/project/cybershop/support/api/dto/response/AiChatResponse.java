package com.project.cybershop.support.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String conversationId;
    private String message;
    private String messageType; // "info", "proposal", "navigate"
    private String messageRoute; // ví dụ: "/don-hang/TZ-8219" hoặc "/bao-hanh"
    @Builder.Default
    private List<QuickAction> quickActions = new ArrayList<>();
    @Builder.Default
    private List<ProductSummary> products = new ArrayList<>();
    private StaffInfo staffInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String label;
        private String action; // "navigate", "ask", "connect_staff", "end_staff_session"
        private String route;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private Integer id;
        private Integer variantId;
        private String name;
        private String brand;
        private String description;
        private String variantName;
        private String category;
        private Double price;
        private Double originalPrice;
        private Integer stock;
        private String imageUrl;
        private String route;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffInfo {
        private Integer id;
        private String name;
        private String role;
        private String email;
        private String phone;
        private String avatar;
        private String status; // "online", "busy"
    }
}
