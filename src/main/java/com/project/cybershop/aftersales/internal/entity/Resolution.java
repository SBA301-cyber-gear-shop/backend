package com.project.cybershop.aftersales.internal.entity;

import com.project.cybershop.shared.exception.BusinessException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Kết quả xử lý của phiếu bảo hành, tách khỏi trạng thái.
 * DB và JSON dùng dạng chữ thường, ví dụ no_fault_found.
 */
public enum Resolution {
    REPAIRED, REPLACED, REJECTED, NO_FAULT_FOUND, WITHDRAWN;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Chuỗi rỗng hoặc null nghĩa là chưa có kết quả. */
    public static Resolution fromCodeOrNull(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Kết quả xử lý không hợp lệ: " + code);
        }
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<Resolution, String> {

        @Override
        public String convertToDatabaseColumn(Resolution resolution) {
            return resolution == null ? null : resolution.code();
        }

        @Override
        public Resolution convertToEntityAttribute(String value) {
            return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
