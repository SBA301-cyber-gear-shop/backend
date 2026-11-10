package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.request.IntakeCreateRequest;
import com.project.cybershop.aftersales.api.dto.request.NoteRequest;
import com.project.cybershop.aftersales.api.dto.request.NoFaultComponentsRequest;
import com.project.cybershop.aftersales.api.dto.request.RelinkCardRequest;
import com.project.cybershop.aftersales.api.dto.response.ServiceIntakeDto;
import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceIntake;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.OrderLookup.OrderInfo;
import com.project.cybershop.aftersales.internal.repository.ServiceIntakeRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.aftersales.internal.service.CurrentUser.Principal;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus.*;

/**
 * Lần tiếp nhận cả bộ máy: khách mang một hoặc nhiều linh kiện của cùng một đơn hàng tới quầy.
 * Mỗi linh kiện là một phiếu con xử lý riêng; tạo, trả máy và hủy làm cho cả lần cùng lúc.
 */
@Service
public class ServiceIntakeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String NO_FAULT_NOTE = "Linh kiện hoạt động bình thường, không phát hiện lỗi.";

    private final ServiceIntakeRepository intakes;
    private final ServiceRequestRepository requests;
    private final ServiceRequestService requestService;
    private final WarrantyCardService cardService;
    private final OrderLookup orderLookup;

    public ServiceIntakeService(ServiceIntakeRepository intakes,
                                ServiceRequestRepository requests,
                                ServiceRequestService requestService,
                                WarrantyCardService cardService,
                                OrderLookup orderLookup) {
        this.intakes = intakes;
        this.requests = requests;
        this.requestService = requestService;
        this.cardService = cardService;
        this.orderLookup = orderLookup;
    }

    @Transactional
    public ServiceIntakeDto create(IntakeCreateRequest req, Principal actor) {
        LocalDateTime now = WarrantyRules.now();
        LocalDate today = now.toLocalDate();
        if (req.getExpectedReturnDate().isBefore(today)) {
            throw new BusinessException("Ngày hẹn trả không được trước ngày tiếp nhận.");
        }
        List<Integer> cardIds = req.getWarrantyCardIds().stream().distinct().toList();
        List<Integer> extraIds = req.getOutOfWarrantyCardIds() == null ? List.of()
                : req.getOutOfWarrantyCardIds().stream().distinct().filter(id -> !cardIds.contains(id)).toList();
        List<Integer> allIds = Stream.concat(cardIds.stream(), extraIds.stream()).toList();
        if (allIds.isEmpty()) {
            throw new BusinessException("Chọn ít nhất một linh kiện để tiếp nhận.");
        }
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(allIds);
        Set<Integer> orderIds = allIds.stream().map(id -> {
            OrderInfo info = orders.get(id);
            if (info == null) {
                throw new BusinessException("Thẻ bảo hành chưa gắn với đơn hàng hợp lệ.");
            }
            return info.orderId();
        }).collect(Collectors.toSet());
        if (orderIds.size() != 1) {
            throw new BusinessException("Các linh kiện tiếp nhận cùng lúc phải thuộc cùng một đơn hàng.");
        }
        List<WarrantyCard> cards = allIds.stream().map(requestService::card).toList();
        for (WarrantyCard card : cards) {
            if (cardIds.contains(card.getId()) && !WarrantyRules.isValidOn(card, today)) {
                throw new BusinessException("Thẻ " + card.getSerialNumber() + " không còn hiệu lực tại thời điểm tiếp nhận.");
            }
            if (extraIds.contains(card.getId()) && (!WarrantyCard.ACTIVE.equals(card.getStatus())
                    || !card.getEndDate().toLocalDate().isBefore(today))) {
                throw new BusinessException("Chỉ nhận ngoài bảo hành với thẻ đang hoạt động và đã hết hạn.");
            }
        }
        String accessories = req.getAccessoriesNote().trim();

        OrderInfo order = orders.get(allIds.get(0));
        ServiceIntake intake = new ServiceIntake();
        intake.setIntakeCode(WarrantyRules.newRequestCode(today, RANDOM));
        intake.setOrderId(order.orderId());
        // Lần tiếp nhận luôn thuộc về chủ đơn hàng, không lấy từ request để tránh gắn nhầm tài khoản.
        intake.setUserId(order.userId());
        intake.setCreatedBy(actor.id());
        intake.setCreatedAt(now);
        save(intake);

        int index = 0;
        for (WarrantyCard card : cards) {
            ServiceRequest request = child(intake, card, ++index);
            request.setContactName(req.getContactName().trim());
            request.setContactPhone(req.getContactPhone().trim());
            request.setIssueDescription(req.getIssueDescription().trim());
            request.setConditionNote(req.getConditionNote().trim());
            request.setAccessoriesNote(accessories);
            request.setExpectedReturnDate(req.getExpectedReturnDate());
            request.setStatus(RECEIVED);
            request.setCreatedAt(now);
            if (extraIds.contains(card.getId())) {
                request.setPublicNote("Đã tiếp nhận linh kiện ngoài bảo hành để kiểm tra.");
            }
            requestService.open(request, actor, "Tiếp nhận tại quầy, lần " + intake.getIntakeCode() + ".", now);
        }
        return detail(intake, actor);
    }

    @Transactional(readOnly = true)
    public ServiceIntakeDto get(Integer id, Principal actor) {
        return detail(load(id), actor);
    }

    /** Trả cả bộ máy một lần, khi mọi linh kiện đã xử lý xong. Hạn bảo hành được cộng cho linh kiện đã sửa. */
    @Transactional
    public ServiceIntakeDto returnAll(Integer id, NoteRequest req, Principal actor) {
        ServiceIntake intake = load(id);
        List<ServiceRequest> parts = parts(intake);
        if (!WarrantyRules.canReturn(statuses(parts))) {
            throw new BusinessException("Chỉ trả máy khi mọi linh kiện đã xử lý xong (chờ trả khách hoặc đã hủy).");
        }
        for (ServiceRequest part : parts) {
            if (part.getStatus() == READY_FOR_RETURN) {
                requestService.move(part, RETURNED, null, req.getInternalNote(), req.getPublicNote(), null, actor);
            }
        }
        intake.setReturnedAt(WarrantyRules.now());
        save(intake);
        return detail(intake, actor);
    }

    /** Hủy cả lần tiếp nhận khi chưa linh kiện nào được kiểm tra, ví dụ nhân viên tạo nhầm. */
    @Transactional
    public ServiceIntakeDto cancel(Integer id, NoteRequest req, Principal actor) {
        ServiceIntake intake = load(id);
        List<ServiceRequest> parts = parts(intake);
        List<ServiceRequestStatus> statuses = statuses(parts);
        if (!canCancel(statuses)) {
            throw new BusinessException("Chỉ hủy cả lần tiếp nhận khi chưa linh kiện nào được kiểm tra.");
        }
        for (ServiceRequest part : parts) {
            if (part.getStatus() == RECEIVED) {
                requestService.move(part, CANCELED, null, req.getInternalNote(), req.getPublicNote(), null, actor);
            }
        }
        return detail(intake, actor);
    }

    /**
     * Kỹ thuật viên phát hiện thêm linh kiện lỗi trong bộ máy khách mang tới: thêm một phiếu con vào cùng lần.
     * Chỉ nhận thẻ cùng đơn hàng, để không gắn nhầm sang máy của khách khác.
     */
    @Transactional
    public ServiceIntakeDto addComponent(Integer id, RelinkCardRequest req, Principal actor) {
        ServiceIntake intake = load(id);
        List<ServiceRequest> parts = parts(intake);
        if (intake.getReturnedAt() != null || parts.stream().noneMatch(p -> p.getStatus().isOpen())) {
            throw new BusinessException("Lần tiếp nhận đã kết thúc, hãy tạo lần tiếp nhận mới.");
        }
        WarrantyCard card = requestService.card(req.getWarrantyCardId());
        if (!requestService.orderInfo(card).orderId().equals(intake.getOrderId())) {
            throw new BusinessException("Chỉ thêm được linh kiện thuộc cùng đơn hàng của lần tiếp nhận.");
        }
        if (!WarrantyCard.ACTIVE.equals(card.getStatus())
                || card.getStartDate().toLocalDate().isAfter(intake.getCreatedAt().toLocalDate())) {
            throw new BusinessException("Thẻ của linh kiện không hoạt động tại thời điểm tiếp nhận.");
        }
        ServiceRequest first = parts.get(0);
        ServiceRequest request = child(intake, card, parts.size() + 1);
        request.setContactName(first.getContactName());
        request.setContactPhone(first.getContactPhone());
        request.setIssueDescription(first.getIssueDescription());
        request.setConditionNote(first.getConditionNote());
        request.setAccessoriesNote(first.getAccessoriesNote());
        request.setExpectedReturnDate(first.getExpectedReturnDate());
        // Linh kiện đã nằm ở tiệm từ lúc tiếp nhận: tính ngày nhận theo lần tiếp nhận để cộng hạn cho đúng.
        request.setCreatedAt(intake.getCreatedAt());
        request.setStatus(INSPECTING);
        if (!WarrantyRules.wasCoveredOn(card, intake.getCreatedAt().toLocalDate())) {
            request.setPublicNote("Đã tiếp nhận linh kiện ngoài bảo hành để kiểm tra.");
        }
        request.setAssignedTechnicianId(actor.id());
        requestService.open(request, actor, "Thêm linh kiện khi kiểm tra: " + card.getSerialNumber() + ". "
                + req.getInternalNote().trim(), WarrantyRules.now());
        return detail(intake, actor);
    }

    /** Chỉ kết luận những phiếu kỹ thuật viên đã chọn và xác nhận. */
    @Transactional
    public ServiceIntakeDto noFaultRest(Integer id, NoFaultComponentsRequest req, Principal actor) {
        ServiceIntake intake = load(id);
        Map<Integer, ServiceRequest> partsById = parts(intake).stream()
                .collect(Collectors.toMap(ServiceRequest::getId, p -> p));
        Set<Integer> selectedIds = new java.util.HashSet<>();
        for (var selected : req.components()) {
            ServiceRequest part = partsById.get(selected.id());
            if (part == null || !selectedIds.add(selected.id())) {
                throw new BusinessException("Danh sách linh kiện không hợp lệ hoặc không thuộc lần tiếp nhận.");
            }
            if (!selected.version().equals(part.getVersion())
                    || (part.getStatus() != RECEIVED && part.getStatus() != INSPECTING)) {
                throw new BusinessException("Linh kiện đã thay đổi trạng thái. Vui lòng tải lại và chọn lại.");
            }
        }
        String publicNote = req.publicNote() == null || req.publicNote().isBlank()
                ? NO_FAULT_NOTE : req.publicNote();
        for (var selected : req.components()) {
            ServiceRequest part = partsById.get(selected.id());
            if (part.getStatus() == RECEIVED) {
                requestService.move(part, INSPECTING, null, req.internalNote(), null, null, actor);
            }
            requestService.move(part, READY_FOR_RETURN, Resolution.NO_FAULT_FOUND, req.internalNote(),
                    publicNote, null, actor);
        }
        return detail(intake, actor);
    }

    private ServiceIntake load(Integer id) {
        return intakes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lần tiếp nhận: " + id));
    }

    private List<ServiceRequest> parts(ServiceIntake intake) {
        return requests.findByIntakeIdOrderByIdAsc(intake.getId());
    }

    private static List<ServiceRequestStatus> statuses(List<ServiceRequest> parts) {
        return parts.stream().map(ServiceRequest::getStatus).toList();
    }

    private static boolean canCancel(List<ServiceRequestStatus> statuses) {
        return statuses.contains(RECEIVED) && statuses.stream().allMatch(s -> s == RECEIVED || s == CANCELED);
    }

    private static ServiceRequest child(ServiceIntake intake, WarrantyCard card, int index) {
        ServiceRequest request = new ServiceRequest();
        request.setRequestCode(WarrantyRules.childCode(intake.getIntakeCode(), index));
        request.setIntakeId(intake.getId());
        request.setUserId(intake.getUserId());
        request.setWarrantyCardId(card.getId());
        request.setPublicNote("Đã tiếp nhận thiết bị.");
        return request;
    }

    private void save(ServiceIntake intake) {
        try {
            intakes.saveAndFlush(intake);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ServiceRequestService.STALE);
        }
    }

    private ServiceIntakeDto detail(ServiceIntake intake, Principal actor) {
        List<ServiceRequest> parts = parts(intake);
        List<ServiceRequestStatus> statuses = statuses(parts);
        ServiceRequest first = parts.get(0);
        OrderInfo order = orderLookup.byCardIds(List.of(first.getWarrantyCardId())).get(first.getWarrantyCardId());
        boolean counter = actor.actor() == Actor.STAFF || actor.actor() == Actor.ADMIN;
        boolean technical = actor.actor() == Actor.TECHNICIAN || actor.actor() == Actor.ADMIN;
        return ServiceIntakeDto.builder()
                .id(intake.getId())
                .intakeCode(intake.getIntakeCode())
                .orderId(intake.getOrderId())
                .orderCode(order == null ? null : order.orderCode())
                .customerName(order == null ? null : order.customerName())
                .status(WarrantyRules.intakeStatus(statuses).code())
                .contactName(first.getContactName())
                .contactPhone(first.getContactPhone())
                .issueDescription(first.getIssueDescription())
                .conditionNote(first.getConditionNote())
                .accessoriesNote(first.getAccessoriesNote())
                .expectedReturnDate(first.getExpectedReturnDate())
                .createdAt(intake.getCreatedAt())
                .returnedAt(intake.getReturnedAt())
                .version(intake.getVersion())
                .requests(cardService.summaries(parts))
                .canReturn(counter && WarrantyRules.canReturn(statuses))
                .canCancel(counter && canCancel(statuses))
                .canAddComponent(technical && intake.getReturnedAt() == null
                        && statuses.stream().anyMatch(ServiceRequestStatus::isOpen))
                .build();
    }
}
