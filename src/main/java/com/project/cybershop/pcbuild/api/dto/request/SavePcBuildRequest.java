package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePcBuildRequest {
    private String buildName;

    @NotNull(message = "Tổng tiền không được để trống")
    private BigDecimal totalPrice;

    @NotEmpty(message = "Danh sách linh kiện không được để trống")
    private List<SavePartItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavePartItem {
        @NotNull(message = "Variant ID không được để trống")
        private Integer variantId;

        private String role;

        @NotNull(message = "Số lượng không được để trống")
        private Integer quantity;

        @NotNull(message = "Đơn giá không được để trống")
        private BigDecimal unitPrice;
    }
}
