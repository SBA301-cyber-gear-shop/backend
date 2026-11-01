package com.project.cybershop.pcbuild.api.dto.request;

import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Request của POST /validate (DD 9.2). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateBuildRequest {
    @DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0")
    @DecimalMax(value = "9999999999", message = "Ngân sách tối đa 9.999.999.999 ₫")
    private BigDecimal budget;

    @Valid
    @NotEmpty(message = "Danh sách linh kiện không được để trống")
    @Size(max = 16, message = "Tối đa 16 dòng linh kiện")
    private List<ItemPart> items;

    // Hồ sơ đang dùng (tùy chọn): có thì sinh check lệch hồ sơ P:* mức warning (DD 7.4)
    private RequirementProfileDto profile;

    public ValidateBuildRequest(BigDecimal budget, List<ItemPart> items) {
        this(budget, items, null);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemPart {
        @NotBlank(message = "Vai trò linh kiện không được để trống")
        private String role;

        @NotNull(message = "Variant ID không được để trống")
        private Integer variantId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Max(value = 8, message = "Số lượng tối đa là 8")
        private Integer quantity;
    }
}
