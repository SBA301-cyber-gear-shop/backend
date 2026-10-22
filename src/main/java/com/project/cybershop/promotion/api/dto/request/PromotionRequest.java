package com.project.cybershop.promotion.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank @Size(max=50) String code,
        @NotBlank @Size(max=150) String name,
        @NotBlank String type,
        @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=2) BigDecimal value,
        @NotNull @Min(1) Integer limit,
        @NotNull LocalDateTime start, @NotNull LocalDateTime end,
        boolean active,
        @Size(max=150) String bannerTitle, @Size(max=255) String bannerImage,
        @Size(max=255) String bannerLink, @Min(0) int bannerOrder, boolean bannerActive
) {}
