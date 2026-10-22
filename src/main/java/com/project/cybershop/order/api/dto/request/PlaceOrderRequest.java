package com.project.cybershop.order.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank @Size(max=100) String pickupName,
        @NotBlank @Size(max=20) String pickupPhone,
        @Size(max=2000) String note,
        @Size(max=50) String promotionCode,
        @NotBlank String method,
        Integer paymentMethodId,
        @NotNull @DecimalMin("0") BigDecimal expectedTotal,
        @NotNull @DecimalMin("0") BigDecimal expectedDiscount
) {}
