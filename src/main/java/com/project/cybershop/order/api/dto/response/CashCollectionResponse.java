package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashCollectionResponse(
        Integer collectorUserId,
        String collectorName,
        LocalDateTime collectedAt,
        BigDecimal receivedAmount,
        BigDecimal changeAmount,
        String reference
) {}
