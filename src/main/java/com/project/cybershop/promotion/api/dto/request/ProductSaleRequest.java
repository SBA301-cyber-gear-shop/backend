package com.project.cybershop.promotion.api.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ProductSaleRequest(boolean enabled,
        @DecimalMin("0.01") @DecimalMax("100") @Digits(integer=3, fraction=2) BigDecimal percentage,
        @NotNull LocalDateTime start, @NotNull LocalDateTime end,
        String discountType, BigDecimal discountValue) {}
