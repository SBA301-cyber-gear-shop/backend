package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class WarrantyRulesTest {

    private static WarrantyCard card(String status) {
        WarrantyCard card = new WarrantyCard();
        card.setStatus(status);
        card.setStartDate(LocalDateTime.of(2026, 8, 1, 9, 30));
        // Ngày hết hạn lưu lúc 00:00 vẫn phải được tính trọn ngày đó.
        card.setEndDate(LocalDateTime.of(2029, 8, 1, 0, 0));
        return card;
    }

    @Test
    void cardIsValidFromStartDayThroughTheWholeEndDay() {
        WarrantyCard card = card(WarrantyCard.ACTIVE);
        assertFalse(WarrantyRules.isValidOn(card, LocalDate.of(2026, 7, 31)));
        assertTrue(WarrantyRules.isValidOn(card, LocalDate.of(2026, 8, 1)));
        assertTrue(WarrantyRules.isValidOn(card, LocalDate.of(2028, 1, 15)));
        assertTrue(WarrantyRules.isValidOn(card, LocalDate.of(2029, 8, 1)));
        assertFalse(WarrantyRules.isValidOn(card, LocalDate.of(2029, 8, 2)));
    }

    @Test
    void onlyActiveCardsAreValid() {
        LocalDate day = LocalDate.of(2027, 1, 1);
        assertFalse(WarrantyRules.isValidOn(card(WarrantyCard.VOID), day));
        assertFalse(WarrantyRules.isValidOn(card(WarrantyCard.REPLACED), day));
        assertFalse(WarrantyRules.isValidOn(card(null), day));
    }

    @Test
    void extensionCountsCalendarDaysFromReceiptToCompletion() {
        // Ví dụ trong tài liệu: nhận 01/03, sửa xong 21/03, cộng 20 ngày.
        assertEquals(20, WarrantyRules.extensionDays(
                LocalDateTime.of(2027, 3, 1, 16, 45), LocalDateTime.of(2027, 3, 21, 8, 5)));
        assertEquals(0, WarrantyRules.extensionDays(
                LocalDateTime.of(2027, 3, 1, 8, 0), LocalDateTime.of(2027, 3, 1, 17, 0)));
        assertEquals(1, WarrantyRules.extensionDays(
                LocalDateTime.of(2027, 3, 1, 23, 50), LocalDateTime.of(2027, 3, 2, 0, 10)));
        assertEquals(0, WarrantyRules.extensionDays(LocalDateTime.of(2027, 3, 1, 8, 0), null));
        assertEquals(0, WarrantyRules.extensionDays(null, LocalDateTime.of(2027, 3, 1, 8, 0)));
        assertEquals(0, WarrantyRules.extensionDays(
                LocalDateTime.of(2027, 3, 5, 8, 0), LocalDateTime.of(2027, 3, 1, 8, 0)));
        // Hạn cũ 10/10/2027 cộng 20 ngày thành 30/10/2027.
        assertEquals(LocalDate.of(2027, 10, 30), LocalDate.of(2027, 10, 10).plusDays(20));
    }

    @Test
    void requestCodeHasDatePrefixAndSixUnambiguousCharacters() {
        Random random = new Random(42);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String code = WarrantyRules.newRequestCode(LocalDate.of(2026, 10, 10), random);
            assertTrue(code.matches("BH-261010-[A-HJKMNP-Z2-9]{6}"), code);
            seen.add(code);
        }
        assertTrue(seen.size() > 495, "mã phải gần như không trùng: " + seen.size());
    }

    @Test
    void serialIsTrimmedAndUppercased() {
        assertEquals("KST26A000428", WarrantyRules.normalizeSerial("  kst26a000428 "));
        assertEquals("", WarrantyRules.normalizeSerial(null));
    }

    @Test
    void childCodeAppendsTwoDigitIndexToIntakeCode() {
        assertEquals("BH-261027-7K2QXM-01", WarrantyRules.childCode("BH-261027-7K2QXM", 1));
        assertEquals("BH-261027-7K2QXM-12", WarrantyRules.childCode("BH-261027-7K2QXM", 12));
    }

    @Test
    void intakeStatusIsTheLeastAdvancedComponentIgnoringCanceled() {
        assertEquals(RECEIVED, WarrantyRules.intakeStatus(List.of(RECEIVED, READY_FOR_RETURN)));
        assertEquals(REPAIRING, WarrantyRules.intakeStatus(List.of(READY_FOR_RETURN, REPAIRING, WAITING_FOR_PARTS)));
        assertEquals(READY_FOR_RETURN, WarrantyRules.intakeStatus(List.of(READY_FOR_RETURN, CANCELED)));
        assertEquals(RETURNED, WarrantyRules.intakeStatus(List.of(RETURNED, CANCELED)));
        assertEquals(CANCELED, WarrantyRules.intakeStatus(List.of(CANCELED, CANCELED)));
    }

    @Test
    void wholeSetReturnsOnlyWhenEveryComponentIsReadyOrCanceled() {
        assertTrue(WarrantyRules.canReturn(List.of(READY_FOR_RETURN, READY_FOR_RETURN)));
        assertTrue(WarrantyRules.canReturn(List.of(READY_FOR_RETURN, CANCELED)));
        assertFalse(WarrantyRules.canReturn(List.of(READY_FOR_RETURN, REPAIRING)));
        // Không còn gì để trả: đã trả rồi, hoặc mọi linh kiện đã hủy.
        assertFalse(WarrantyRules.canReturn(List.of(RETURNED, RETURNED)));
        assertFalse(WarrantyRules.canReturn(List.of(CANCELED)));
    }
}
