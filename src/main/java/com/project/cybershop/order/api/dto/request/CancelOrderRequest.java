package com.project.cybershop.order.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank(message = "Vui lòng nhập lý do hủy đơn")
        @Size(max = 500, message = "Lý do hủy đơn tối đa 500 ký tự") String reason
) {}
