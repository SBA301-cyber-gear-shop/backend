package com.project.cybershop.order.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CollectPaymentRequest(
        @NotNull(message = "Vui lòng nhập số tiền đã nhận")
        @DecimalMin(value = "0", message = "Số tiền đã nhận không được âm")
        @DecimalMax(value = "9007199254740991", message = "Số tiền đã nhận vượt quá giới hạn")
        @Digits(integer = 16, fraction = 0, message = "Số tiền VND phải là số nguyên tối đa 16 chữ số")
        BigDecimal receivedAmount,
        @Size(max=255, message = "Mã tham chiếu tối đa 255 ký tự") String reference
) {}
