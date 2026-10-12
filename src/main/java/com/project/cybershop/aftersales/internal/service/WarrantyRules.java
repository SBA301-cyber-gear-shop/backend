package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.internal.entity.WarrantyCard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Các quy tắc tính toán của bảo hành, không phụ thuộc DB.
 */
public final class WarrantyRules {

    // Mọi thời điểm của module tính theo giờ Việt Nam, không phụ thuộc múi giờ của máy chạy BE hay của DB.
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter CODE_DAY = DateTimeFormatter.ofPattern("yyMMdd");
    // Bỏ các ký tự dễ nhầm khi đọc biên nhận: 0 và O, 1 và I và L.
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private WarrantyRules() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** Thẻ còn hiệu lực từ ngày bắt đầu đến hết ngày kết thúc. */
    public static boolean isValidOn(WarrantyCard card, LocalDate day) {
        return WarrantyCard.ACTIVE.equals(card.getStatus())
                && !day.isBefore(card.getStartDate().toLocalDate())
                && !day.isAfter(card.getEndDate().toLocalDate());
    }

    /** Số ngày cộng vào hạn bảo hành: từ ngày tiếp nhận đến ngày sửa xong. */
    public static long extensionDays(LocalDateTime receivedAt, LocalDateTime completedAt) {
        if (receivedAt == null || completedAt == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(receivedAt.toLocalDate(), completedAt.toLocalDate()));
    }

    /** Mã in trên biên nhận, ví dụ BH-261010-7K2QXM. Phần cuối ngẫu nhiên để không đoán được. */
    public static String newRequestCode(LocalDate day, RandomGenerator random) {
        StringBuilder code = new StringBuilder("BH-").append(day.format(CODE_DAY)).append('-');
        for (int i = 0; i < 6; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    public static String normalizeSerial(String serial) {
        return serial == null ? "" : serial.trim().toUpperCase(Locale.ROOT);
    }
}
