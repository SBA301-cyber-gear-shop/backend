package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Một món khách đang chọn: id biến thể và số lượng. */
public record BuildItemRequest(
        @NotNull(message = "variantId không được để trống")
        Integer variantId,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Max(value = 8, message = "Số lượng tối đa là 8")
        Integer qty
) {
}
