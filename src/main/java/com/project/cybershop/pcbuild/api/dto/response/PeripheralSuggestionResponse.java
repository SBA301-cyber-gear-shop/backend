package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Response POST /peripherals: 3 món được chọn, các lựa chọn thay thế và các tổng tiền. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeripheralSuggestionResponse {
    private BigDecimal pcTotal;
    private BigDecimal available;       // ngân sách - giá thùng máy
    private BigDecimal spendLimit;
    private BigDecimal peripheralTotal;
    private BigDecimal grandTotal;
    private List<RoleEntry> roles;      // thứ tự hiển thị: màn, phím, chuột

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleEntry {
        private String role;
        private String label;
        private Item picked;
        private List<Item> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Integer variantId;
        private String productName;
        private String variantName;
        private String brandName;
        private String imageUrl;
        private BigDecimal price;
        private Integer stockQuantity;
        private List<String> highlights;
        private String reason;          // chỉ món được chọn mới có
    }
}
