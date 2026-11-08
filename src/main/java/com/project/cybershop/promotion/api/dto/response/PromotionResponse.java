package com.project.cybershop.promotion.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(
        Integer id, String code, String name, String type, BigDecimal value, int limit,
        LocalDateTime start, LocalDateTime end, boolean active, int used, int reserved,
        String bannerTitle, String bannerImage, String bannerLink, int bannerOrder, boolean bannerActive, String audience
) {}
