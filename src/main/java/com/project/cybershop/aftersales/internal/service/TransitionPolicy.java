package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static com.project.cybershop.aftersales.internal.entity.Resolution.*;
import static com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus.*;
import static com.project.cybershop.aftersales.internal.service.Actor.STAFF;
import static com.project.cybershop.aftersales.internal.service.Actor.TECHNICIAN;

/**
 * Bảng chuyển trạng thái của phiếu bảo hành: từ đâu sang đâu, ai được làm, kèm kết quả nào.
 * Admin làm được mọi bước. Kết quả REPLACED chỉ đến từ thao tác đổi thiết bị, không nằm trong bảng này.
 */
public final class TransitionPolicy {

    public record Rule(ServiceRequestStatus from, ServiceRequestStatus to, Actor actor, List<Resolution> resolutions) {
        boolean allows(Actor who) {
            return who == actor || who == Actor.ADMIN;
        }
    }

    private static final List<Rule> RULES = List.of(
            rule(RECEIVED, INSPECTING, TECHNICIAN),
            rule(RECEIVED, CANCELED, STAFF),
            rule(INSPECTING, REPAIRING, TECHNICIAN),
            rule(INSPECTING, READY_FOR_RETURN, TECHNICIAN, REJECTED, NO_FAULT_FOUND, WITHDRAWN),
            rule(INSPECTING, CANCELED, TECHNICIAN),
            rule(REPAIRING, READY_FOR_RETURN, TECHNICIAN, REPAIRED, WITHDRAWN),
            rule(REPAIRING, WAITING_FOR_PARTS, TECHNICIAN),
            rule(REPAIRING, INSPECTING, TECHNICIAN),
            rule(WAITING_FOR_PARTS, REPAIRING, TECHNICIAN),
            rule(WAITING_FOR_PARTS, READY_FOR_RETURN, TECHNICIAN, WITHDRAWN),
            rule(READY_FOR_RETURN, RETURNED, STAFF),
            rule(READY_FOR_RETURN, INSPECTING, STAFF));

    private TransitionPolicy() {
    }

    private static Rule rule(ServiceRequestStatus from, ServiceRequestStatus to, Actor actor, Resolution... resolutions) {
        return new Rule(from, to, actor, List.of(resolutions));
    }

    /** Các bước người này được làm tiếp từ trạng thái hiện tại. */
    public static List<Rule> allowedFor(ServiceRequestStatus from, Actor actor) {
        return RULES.stream().filter(r -> r.from() == from && r.allows(actor)).toList();
    }

    public static void check(ServiceRequestStatus from, ServiceRequestStatus to, Resolution resolution, Actor actor) {
        Rule rule = RULES.stream()
                .filter(r -> r.from() == from && r.to() == to)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Chuyển trạng thái bảo hành không hợp lệ."));
        if (!rule.allows(actor)) {
            throw new AccessDeniedException("Vai trò của bạn không được thực hiện bước này.");
        }
        // List.of(...).contains(null) ném NullPointerException, nên phải loại null trước.
        boolean resolutionOk = rule.resolutions().isEmpty()
                ? resolution == null
                : resolution != null && rule.resolutions().contains(resolution);
        if (!resolutionOk) {
            throw new BusinessException("Kết quả xử lý không hợp lệ cho bước này.");
        }
    }
}
