package com.project.cybershop.order.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchAddToCartRequest(
        @NotEmpty(message = "Danh sách linh kiện không được để trống")
        List<@Valid AddToCartRequest> items
) {
}
