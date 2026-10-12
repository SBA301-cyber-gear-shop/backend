package com.project.cybershop.aftersales.internal.entity;

import com.project.cybershop.shared.exception.BusinessException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Trạng thái của phiếu bảo hành: thiết bị đang ở khâu nào.
 * DB và JSON dùng dạng chữ thường, ví dụ ready_for_return.
 */
public enum ServiceRequestStatus {
    RECEIVED, INSPECTING, REPAIRING, WAITING_FOR_PARTS, READY_FOR_RETURN, RETURNED, CANCELED;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Phiếu đang mở là phiếu chưa trả khách và chưa hủy. */
    public boolean isOpen() {
        return this != RETURNED && this != CANCELED;
    }

    public static ServiceRequestStatus fromCode(String code) {
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new BusinessException("Trạng thái không hợp lệ: " + code);
        }
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<ServiceRequestStatus, String> {

        @Override
        public String convertToDatabaseColumn(ServiceRequestStatus status) {
            return status == null ? null : status.code();
        }

        @Override
        public ServiceRequestStatus convertToEntityAttribute(String value) {
            return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
