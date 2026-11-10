package com.project.cybershop.pcbuild.internal.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSlotRequest {
    @NotNull(message = "Danh mục linh kiện không được để trống")
    private Integer categoryId;

    @NotNull(message = "Thứ tự bước lắp ráp không được để trống")
    @Min(value = 1, message = "Thứ tự bước phải >= 1")
    private Integer stepOrder;

    @Builder.Default
    private boolean required = true;

    @NotNull(message = "Số lượng tối thiểu không được để trống")
    @Min(value = 0, message = "Số lượng tối thiểu phải >= 0")
    private Integer minQty;

    @NotNull(message = "Số lượng tối đa không được để trống")
    @Min(value = 1, message = "Số lượng tối đa phải >= 1")
    private Integer maxQty;

    // Bộ 3 điều kiện kích hoạt
    private Integer requiredIfCategoryId;
    private Integer requiredIfSpecId;
    private String requiredIfValue;
}
