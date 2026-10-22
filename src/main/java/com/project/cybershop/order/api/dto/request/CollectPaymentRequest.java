package com.project.cybershop.order.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CollectPaymentRequest(
        @NotNull @DecimalMin("0") BigDecimal receivedAmount,
        @Size(max=255) String reference
) {}
