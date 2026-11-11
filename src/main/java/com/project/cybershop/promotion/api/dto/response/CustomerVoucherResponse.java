package com.project.cybershop.promotion.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerVoucherResponse(Integer id, String code, String name, String type, BigDecimal value,
        LocalDateTime start, LocalDateTime end, String status, String orderCode, LocalDateTime grantedAt) {}
