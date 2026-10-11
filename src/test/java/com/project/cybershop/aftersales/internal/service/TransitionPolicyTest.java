package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

import static com.project.cybershop.aftersales.internal.entity.Resolution.*;
import static com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus.*;
import static com.project.cybershop.aftersales.internal.service.Actor.*;
import static org.junit.jupiter.api.Assertions.*;

class TransitionPolicyTest {

    /** Bảng ở mục 3 của DB/aftersales-changes.md: từ, đến, vai trò, các kết quả được chọn. */
    private record Row(ServiceRequestStatus from, ServiceRequestStatus to, Actor actor, List<Resolution> resolutions) {
    }

    private static final List<Row> TABLE = List.of(
            new Row(RECEIVED, INSPECTING, TECHNICIAN, List.of()),
            new Row(RECEIVED, CANCELED, STAFF, List.of()),
            new Row(INSPECTING, CANCELED, TECHNICIAN, List.of()),
            new Row(INSPECTING, REPAIRING, TECHNICIAN, List.of()),
            new Row(REPAIRING, WAITING_FOR_PARTS, TECHNICIAN, List.of()),
            new Row(WAITING_FOR_PARTS, REPAIRING, TECHNICIAN, List.of()),
            new Row(REPAIRING, INSPECTING, TECHNICIAN, List.of()),
            new Row(INSPECTING, READY_FOR_RETURN, TECHNICIAN, List.of(REJECTED, NO_FAULT_FOUND, WITHDRAWN)),
            new Row(REPAIRING, READY_FOR_RETURN, TECHNICIAN, List.of(REPAIRED, WITHDRAWN)),
            new Row(WAITING_FOR_PARTS, READY_FOR_RETURN, TECHNICIAN, List.of(WITHDRAWN)),
            new Row(READY_FOR_RETURN, RETURNED, STAFF, List.of()),
            new Row(READY_FOR_RETURN, INSPECTING, STAFF, List.of()));

    private static Row row(ServiceRequestStatus from, ServiceRequestStatus to) {
        return TABLE.stream().filter(r -> r.from() == from && r.to() == to).findFirst().orElse(null);
    }

    @Test
    void everyPairOfStatusesIsAllowedExactlyWhenTheTableSaysSo() {
        for (ServiceRequestStatus from : ServiceRequestStatus.values()) {
            for (ServiceRequestStatus to : ServiceRequestStatus.values()) {
                Row row = row(from, to);
                if (row == null) {
                    assertThrows(BusinessException.class, () -> TransitionPolicy.check(from, to, null, ADMIN),
                            from + " -> " + to + " phải bị từ chối");
                } else {
                    Resolution resolution = row.resolutions().isEmpty() ? null : row.resolutions().get(0);
                    assertDoesNotThrow(() -> TransitionPolicy.check(from, to, resolution, row.actor()),
                            from + " -> " + to);
                }
            }
        }
    }

    @Test
    void onlyTheOwningRoleAndAdminMayPerformAStep() {
        for (Row row : TABLE) {
            Resolution resolution = row.resolutions().isEmpty() ? null : row.resolutions().get(0);
            for (Actor actor : Actor.values()) {
                boolean allowed = actor == row.actor() || actor == ADMIN;
                String step = row.from() + " -> " + row.to() + " bởi " + actor;
                if (allowed) {
                    assertDoesNotThrow(() -> TransitionPolicy.check(row.from(), row.to(), resolution, actor), step);
                } else {
                    assertThrows(AccessDeniedException.class,
                            () -> TransitionPolicy.check(row.from(), row.to(), resolution, actor), step);
                }
            }
        }
    }

    @Test
    void resolutionMustMatchTheStep() {
        for (Row row : TABLE) {
            List<Resolution> candidates = new ArrayList<>(List.of(Resolution.values()));
            candidates.add(null);
            for (Resolution resolution : candidates) {
                boolean ok = row.resolutions().isEmpty()
                        ? resolution == null
                        : resolution != null && row.resolutions().contains(resolution);
                String step = row.from() + " -> " + row.to() + " với " + resolution;
                if (ok) {
                    assertDoesNotThrow(() -> TransitionPolicy.check(row.from(), row.to(), resolution, ADMIN), step);
                } else {
                    assertThrows(BusinessException.class,
                            () -> TransitionPolicy.check(row.from(), row.to(), resolution, ADMIN), step);
                }
            }
        }
    }

    @Test
    void replacedIsNeverReachableThroughAPlainTransition() {
        for (Row row : TABLE) {
            assertThrows(BusinessException.class,
                    () -> TransitionPolicy.check(row.from(), row.to(), REPLACED, ADMIN));
        }
    }

    @Test
    void allowedStepsAreListedPerRole() {
        assertEquals(List.of(INSPECTING), targets(RECEIVED, TECHNICIAN));
        assertEquals(List.of(CANCELED), targets(RECEIVED, STAFF));
        assertEquals(List.of(INSPECTING, CANCELED), targets(RECEIVED, ADMIN));
        assertEquals(List.of(RETURNED, INSPECTING), targets(READY_FOR_RETURN, STAFF));
        assertEquals(List.of(), targets(READY_FOR_RETURN, TECHNICIAN));
        for (ServiceRequestStatus status : ServiceRequestStatus.values()) {
            assertEquals(List.of(), targets(status, CUSTOMER), "khách không được chuyển trạng thái");
        }
        assertEquals(List.of(), targets(RETURNED, ADMIN));
        assertEquals(List.of(), targets(CANCELED, ADMIN));
    }

    @Test
    void closedStatusesAreExactlyReturnedAndCanceled() {
        for (ServiceRequestStatus status : ServiceRequestStatus.values()) {
            assertEquals(status != RETURNED && status != CANCELED, status.isOpen(), status.toString());
        }
    }

    @Test
    void codesAreLowercaseAndRoundTrip() {
        assertEquals("ready_for_return", READY_FOR_RETURN.code());
        assertEquals(WAITING_FOR_PARTS, ServiceRequestStatus.fromCode(" waiting_for_parts "));
        assertThrows(BusinessException.class, () -> ServiceRequestStatus.fromCode("completed"));
        assertThrows(BusinessException.class, () -> ServiceRequestStatus.fromCode(null));
        assertEquals("no_fault_found", NO_FAULT_FOUND.code());
        assertEquals(REPAIRED, Resolution.fromCodeOrNull("repaired"));
        assertNull(Resolution.fromCodeOrNull(" "));
        assertNull(Resolution.fromCodeOrNull(null));
        assertThrows(BusinessException.class, () -> Resolution.fromCodeOrNull("refunded"));
    }

    private static List<ServiceRequestStatus> targets(ServiceRequestStatus from, Actor actor) {
        return TransitionPolicy.allowedFor(from, actor).stream().map(TransitionPolicy.Rule::to).toList();
    }
}
