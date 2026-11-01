package com.project.cybershop.pcbuild.internal.model;

import java.util.Locale;

/** Nhãn hồ sơ mẫu gần nhất (DD 5.3, 5.5); không phải danh sách nhu cầu khách chọn. */
public enum Archetype {
    GAMING, CREATIVE, AI_DATA, OFFICE, DEV, GENERAL;

    /** Giá trị lạ hoặc null rơi về GENERAL. */
    public static Archetype parse(String value) {
        if (value == null) return GENERAL;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
