package com.project.cybershop.pcbuild.internal.model;

import java.util.Locale;
import java.util.Optional;

/** Chỉ số cấp bộ máy (DD 5.2): phép cộng trên build, vai trò gắn điểm, đơn vị và nhãn hiển thị. */
public enum MetricKind {
    TOTAL_RAM_GB("ram", "GB", "Tổng RAM"),
    TOTAL_STORAGE_GB("ssd", "GB", "Tổng dung lượng SSD"),
    TOTAL_VRAM_GB("gpu", "GB", "Tổng VRAM"),
    GPU_COUNT("gpu", null, "Số card đồ họa"),
    CPU_COUNT("cpu", null, "Số CPU"),
    TOTAL_CPU_CORES("cpu", null, "Tổng số nhân CPU");

    private final String role;
    private final String unit;
    private final String label;

    MetricKind(String role, String unit, String label) {
        this.role = role;
        this.unit = unit;
        this.label = label;
    }

    public String role() {
        return role;
    }

    public String unit() {
        return unit;
    }

    public String label() {
        return label;
    }

    public static Optional<MetricKind> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
