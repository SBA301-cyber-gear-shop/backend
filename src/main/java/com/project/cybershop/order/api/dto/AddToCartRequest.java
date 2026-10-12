
package com.project.cybershop.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(

        @NotNull(message = "Vui lòng chọn phiên bản sản phẩm")
        @Min(value = 1, message = "ID phiên bản phải lớn hơn 0")
        Integer productVariantId,

        @NotNull(message = "Vui lòng nhập số lượng")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        Integer quantity

) {
}
