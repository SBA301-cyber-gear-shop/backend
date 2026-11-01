package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

    // Tùy chọn và bị bỏ qua: server tự tính từ giá catalog (giữ để FE cũ không vỡ)
    private BigDecimal totalPrice;

    @Valid
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
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;

        // Tùy chọn và bị bỏ qua: server tự lấy giá từ catalog
        private BigDecimal unitPrice;
    }
}
