package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.response.AllowedTransitionDto;
import com.project.cybershop.aftersales.api.dto.request.NoteRequest;
import com.project.cybershop.aftersales.api.dto.request.RelinkCardRequest;
import com.project.cybershop.aftersales.api.dto.request.ReplacementRequest;
import com.project.cybershop.aftersales.api.dto.request.ServiceRequestCreateRequest;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestEventDto;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestSummaryDto;
import com.project.cybershop.aftersales.api.dto.request.TransitionRequest;
import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestEvent;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.OrderLookup.OrderInfo;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestEventRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.aftersales.internal.repository.WarrantyCardRepository;
import com.project.cybershop.aftersales.internal.service.CurrentUser.Principal;
import com.project.cybershop.aftersales.internal.mapper.AftersalesMapper;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus.*;

/**
 * Nghiệp vụ của phiếu bảo hành. Mỗi thao tác chạy trong một transaction và luôn ghi một dòng lịch sử.
 */
@Service
public class ServiceRequestService {

    private static final List<ServiceRequestStatus> CLOSED = List.of(RETURNED, CANCELED);
    private static final Set<ServiceRequestStatus> IN_PROGRESS = EnumSet.of(INSPECTING, REPAIRING, WAITING_FOR_PARTS);
    private static final String OPEN_EXISTS = "Thiết bị đã có phiếu chưa đóng.";
    private static final String STALE = "Phiếu đã được người khác cập nhật, vui lòng tải lại.";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ServiceRequestRepository requests;
    private final ServiceRequestEventRepository events;
    private final WarrantyCardRepository cards;
    private final OrderLookup orderLookup;
    private final WarrantyCardService cardService;
    private final AftersalesMapper mapper;
    private final IdentityApi identityApi;

    public ServiceRequestService(ServiceRequestRepository requests,
                                 ServiceRequestEventRepository events,
                                 WarrantyCardRepository cards,
                                 OrderLookup orderLookup,
                                 WarrantyCardService cardService,
                                 AftersalesMapper mapper,
                                 IdentityApi identityApi) {
        this.requests = requests;
        this.events = events;
        this.cards = cards;
        this.orderLookup = orderLookup;
        this.cardService = cardService;
        this.mapper = mapper;
        this.identityApi = identityApi;
    }

    @Transactional
    public ServiceRequestDto create(ServiceRequestCreateRequest req, Principal actor) {
        WarrantyCard card = card(req.getWarrantyCardId());
        LocalDateTime now = WarrantyRules.now();
        // Hiệu lực thẻ chỉ xét ở đây. Thẻ hết hạn trong lúc sửa không ảnh hưởng phiếu đang xử lý.
        if (!WarrantyRules.isValidOn(card, now.toLocalDate())) {
            throw new BusinessException("Thẻ không còn hiệu lực tại thời điểm tiếp nhận.");
        }
        if (requests.existsByWarrantyCardIdAndStatusNotIn(card.getId(), CLOSED)) {
            throw new BusinessException(OPEN_EXISTS);
        }
        if (req.getExpectedReturnDate().isBefore(now.toLocalDate())) {
            throw new BusinessException("Ngày hẹn trả không được trước ngày tiếp nhận.");
        }

        ServiceRequest request = new ServiceRequest();
        request.setRequestCode(WarrantyRules.newRequestCode(now.toLocalDate(), RANDOM));
        // Phiếu luôn thuộc về chủ đơn hàng, không lấy từ request để tránh gắn nhầm tài khoản.
        request.setUserId(orderInfo(card).userId());
        request.setWarrantyCardId(card.getId());
        request.setContactName(req.getContactName().trim());
        request.setContactPhone(req.getContactPhone().trim());
        request.setIssueDescription(req.getIssueDescription().trim());
        request.setConditionNote(req.getConditionNote().trim());
        request.setAccessoriesNote(req.getAccessoriesNote().trim());
        request.setExpectedReturnDate(req.getExpectedReturnDate());
        request.setStatus(RECEIVED);
        request.setPublicNote("Đã tiếp nhận thiết bị.");
        request.setCreatedAt(now);
        save(request);
        log(request, actor, null, "Tiếp nhận thiết bị tại quầy.", request.getPublicNote(), now);
        return detail(request, actor);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestSummaryDto> list(String status) {
        // shortcut: chưa phân trang, thêm khi số phiếu lớn.
        List<ServiceRequest> found = status == null || status.isBlank()
                ? requests.findAllByOrderByCreatedAtDescIdDesc()
                : requests.findByStatusOrderByCreatedAtDescIdDesc(ServiceRequestStatus.fromCode(status));
        return cardService.summaries(found);
    }

    @Transactional(readOnly = true)
    public ServiceRequestDto get(Integer id, Principal actor) {
        return detail(load(id), actor);
    }

    @Transactional
    public ServiceRequestDto transition(Integer id, TransitionRequest req, Principal actor) {
        ServiceRequest request = load(id);
        if (req.getVersion() != null && !req.getVersion().equals(request.getVersion())) {
            throw new BusinessException(STALE);
        }
        ServiceRequestStatus from = request.getStatus();
        ServiceRequestStatus to = ServiceRequestStatus.fromCode(req.getToStatus());
        Resolution resolution = Resolution.fromCodeOrNull(req.getResolution());
        TransitionPolicy.check(from, to, resolution, actor.actor());

        WarrantyCard card = card(request.getWarrantyCardId());
        // Sau khi đổi thẻ ở bước chẩn đoán, linh kiện lỗi có thể thuộc một thẻ đã hết hạn.
        boolean underWarranty = to == REPAIRING || resolution == Resolution.REPAIRED || resolution == Resolution.NO_FAULT_FOUND;
        if (underWarranty && !WarrantyRules.isValidOn(card, receivedDay(request))) {
            throw new BusinessException("Thẻ đang gắn không còn hiệu lực lúc tiếp nhận, chỉ có thể từ chối bảo hành.");
        }
        if (reinspectsReplaced(request, to)) {
            throw new BusinessException("Thiết bị đã được đổi. Nếu thiết bị mới có lỗi, trả máy rồi tạo phiếu trên thẻ của thiết bị mới.");
        }
        String internalNote = req.getInternalNote().trim();
        String publicNote = blankToNull(req.getPublicNote());
        if (to == READY_FOR_RETURN && publicNote == null) {
            throw new BusinessException("Nhập nội dung thông báo cho khách khi chuyển sang chờ trả.");
        }

        LocalDateTime now = WarrantyRules.now();
        if (from == RECEIVED && to == INSPECTING) {
            request.setAssignedTechnicianId(actor.id());
        }
        if (to == READY_FOR_RETURN) {
            request.setResolution(resolution);
            request.setCompletedAt(now);
        }
        if (from == READY_FOR_RETURN && to == INSPECTING) {
            request.setResolution(null);
            request.setCompletedAt(null);
        }
        if (to == RETURNED && request.getResolution() == Resolution.REPAIRED) {
            // Cộng một lần lúc trả máy, nên không cộng trùng khi phiếu đã qua nhiều lần kiểm tra lại.
            long days = WarrantyRules.extensionDays(request.getCreatedAt(), request.getCompletedAt());
            if (days > 0) {
                LocalDateTime oldEnd = card.getEndDate();
                card.setEndDate(oldEnd.plusDays(days));
                internalNote += " Cộng " + days + " ngày bảo hành: " + oldEnd.format(DAY)
                        + " thành " + card.getEndDate().format(DAY) + ".";
            }
        }
        request.setStatus(to);
        apply(request, internalNote, publicNote, req.getExpectedReturnDate());
        save(request);
        log(request, actor, from, internalNote, publicNote, now);
        return detail(request, actor);
    }

    @Transactional
    public ServiceRequestDto addNote(Integer id, NoteRequest req, Principal actor) {
        ServiceRequest request = load(id);
        if (!IN_PROGRESS.contains(request.getStatus())) {
            throw new BusinessException("Chỉ ghi chú được khi phiếu đang kiểm tra, đang sửa hoặc chờ linh kiện.");
        }
        String internalNote = req.getInternalNote().trim();
        String publicNote = blankToNull(req.getPublicNote());
        apply(request, internalNote, publicNote, req.getExpectedReturnDate());
        save(request);
        log(request, actor, request.getStatus(), internalNote, publicNote, WarrantyRules.now());
        return detail(request, actor);
    }

    /** Khách mang cả bộ máy: lúc tiếp nhận chưa biết linh kiện nào lỗi, kỹ thuật viên gắn lại đúng thẻ. */
    @Transactional
    public ServiceRequestDto relinkCard(Integer id, RelinkCardRequest req, Principal actor) {
        ServiceRequest request = load(id);
        if (request.getStatus() != INSPECTING) {
            throw new BusinessException("Chỉ đổi thẻ được khi phiếu đang kiểm tra.");
        }
        WarrantyCard current = card(request.getWarrantyCardId());
        WarrantyCard target = card(req.getWarrantyCardId());
        if (target.getId().equals(current.getId())) {
            throw new BusinessException("Phiếu đang gắn đúng thẻ này.");
        }
        if (!WarrantyCard.ACTIVE.equals(target.getStatus())) {
            throw new BusinessException("Thẻ được chọn không còn hoạt động.");
        }
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(List.of(current.getId(), target.getId()));
        OrderInfo currentOrder = orders.get(current.getId());
        OrderInfo targetOrder = orders.get(target.getId());
        if (currentOrder == null || targetOrder == null || !currentOrder.orderId().equals(targetOrder.orderId())) {
            throw new BusinessException("Chỉ đổi được sang thẻ thuộc cùng đơn hàng.");
        }
        if (requests.existsByWarrantyCardIdAndStatusNotIn(target.getId(), CLOSED)) {
            throw new BusinessException("Thẻ được chọn đang có phiếu khác chưa đóng.");
        }

        String internalNote = "Đổi thẻ: " + current.getSerialNumber() + " sang " + target.getSerialNumber()
                + ". " + req.getInternalNote().trim();
        request.setWarrantyCardId(target.getId());
        apply(request, internalNote, null, null);
        save(request);
        log(request, actor, request.getStatus(), internalNote, null, WarrantyRules.now());
        return detail(request, actor);
    }

    /** Đổi sang thiết bị có serial khác: tạo thẻ mới trỏ về thẻ cũ, phiếu vẫn gắn thẻ cũ để giữ lịch sử. */
    @Transactional
    public ServiceRequestDto replaceDevice(Integer id, ReplacementRequest req, Principal actor) {
        ServiceRequest request = load(id);
        if (request.getStatus() != REPAIRING) {
            throw new BusinessException("Chỉ đổi thiết bị được khi phiếu đang sửa chữa.");
        }
        WarrantyCard old = card(request.getWarrantyCardId());
        if (!WarrantyRules.isValidOn(old, receivedDay(request))) {
            throw new BusinessException("Thẻ đang gắn không còn hiệu lực lúc tiếp nhận, không thể đổi thiết bị theo bảo hành.");
        }
        String serial = WarrantyRules.normalizeSerial(req.getNewSerialNumber());
        if (cards.existsBySerialNumberIgnoreCase(serial)) {
            throw new BusinessException("Serial đã có thẻ bảo hành.");
        }
        Integer months = orderInfo(old).warrantyPeriodMonths();
        LocalDateTime now = WarrantyRules.now();

        // Hạn của thiết bị mới tính lại từ ngày đổi, đủ số tháng bảo hành của sản phẩm.
        WarrantyCard replacement = new WarrantyCard();
        replacement.setOrderItemId(old.getOrderItemId());
        replacement.setSerialNumber(serial);
        replacement.setStartDate(now);
        replacement.setEndDate(now.plusMonths(months == null ? 0 : months));
        replacement.setReplacesCardId(old.getId());
        try {
            cards.saveAndFlush(replacement);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Serial đã có thẻ bảo hành, hoặc thiết bị này đã được đổi trước đó.");
        }
        old.setStatus(WarrantyCard.REPLACED);

        // shortcut: chưa tự trừ tồn kho vì chưa có module catalog, ghi nguồn để Admin trừ tay.
        String source = "stock".equals(req.getSource()) ? "hàng đang bán, Admin cần trừ tồn kho" : "hãng gửi về";
        String internalNote = "Đổi thiết bị: " + old.getSerialNumber() + " sang " + serial
                + ". Nguồn: " + source + ". " + req.getInternalNote().trim();
        ServiceRequestStatus from = request.getStatus();
        request.setStatus(READY_FOR_RETURN);
        request.setResolution(Resolution.REPLACED);
        request.setCompletedAt(now);
        apply(request, internalNote, req.getPublicNote().trim(), null);
        save(request);
        log(request, actor, from, internalNote, request.getPublicNote(), now);
        return detail(request, actor);
    }

    private ServiceRequest load(Integer id) {
        return requests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu bảo hành: " + id));
    }

    private WarrantyCard card(Integer id) {
        return cards.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ bảo hành: " + id));
    }

    private OrderInfo orderInfo(WarrantyCard card) {
        OrderInfo info = orderLookup.byCardIds(List.of(card.getId())).get(card.getId());
        if (info == null) {
            throw new BusinessException("Thẻ bảo hành chưa gắn với đơn hàng hợp lệ.");
        }
        return info;
    }

    private static LocalDate receivedDay(ServiceRequest request) {
        return (request.getCreatedAt() == null ? WarrantyRules.now() : request.getCreatedAt()).toLocalDate();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    /** Phiếu đã đổi thiết bị không kiểm tra lại được: phiếu gắn thẻ cũ, thẻ đó đã bị thay. */
    private static boolean reinspectsReplaced(ServiceRequest request, ServiceRequestStatus to) {
        return request.getStatus() == READY_FOR_RETURN && to == INSPECTING
                && request.getResolution() == Resolution.REPLACED;
    }

    /** Phiếu chỉ giữ ghi chú mới nhất, từng lần ghi nằm trong lịch sử. */
    private static void apply(ServiceRequest request, String internalNote, String publicNote, LocalDate expectedReturnDate) {
        request.setTechnicianNote(internalNote);
        if (publicNote != null) {
            request.setPublicNote(publicNote);
        }
        if (expectedReturnDate != null && !expectedReturnDate.equals(request.getExpectedReturnDate())) {
            // Lúc tạo phiếu đã chặn ngày trong quá khứ, đổi ngày ở các bước sau cũng phải chặn.
            if (expectedReturnDate.isBefore(WarrantyRules.now().toLocalDate())) {
                throw new BusinessException("Ngày hẹn trả không được trước hôm nay.");
            }
            request.setExpectedReturnDate(expectedReturnDate);
        }
    }

    private void save(ServiceRequest request) {
        try {
            requests.saveAndFlush(request);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(STALE);
        } catch (DataIntegrityViolationException e) {
            // Chốt cuối khi hai người cùng tạo phiếu hoặc cùng đổi sang một thẻ.
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_service_requests_open_card")) {
                throw new BusinessException(OPEN_EXISTS);
            }
            throw e;
        }
    }

    private void log(ServiceRequest request, Principal actor, ServiceRequestStatus from,
                     String internalNote, String publicNote, LocalDateTime at) {
        ServiceRequestEvent event = new ServiceRequestEvent();
        event.setServiceRequestId(request.getId());
        event.setActorId(actor.id());
        event.setFromStatus(from);
        event.setToStatus(request.getStatus());
        event.setInternalNote(internalNote);
        event.setPublicNote(publicNote);
        event.setCreatedAt(at);
        events.save(event);
    }

    private ServiceRequestDto detail(ServiceRequest request, Principal actor) {
        ServiceRequestDto dto = mapper.toDetail(request);
        dto.setCard(cardService.toDtos(List.of(card(request.getWarrantyCardId()))).get(0));

        List<ServiceRequestEvent> history = events.findByServiceRequestIdOrderByCreatedAtAscIdAsc(request.getId());
        Map<Integer, String> names = new HashMap<>();
        dto.setEvents(history.stream().map(event -> {
            ServiceRequestEventDto eventDto = mapper.toDto(event);
            eventDto.setActorName(names.computeIfAbsent(event.getActorId(), this::userName));
            return eventDto;
        }).toList());
        if (request.getAssignedTechnicianId() != null) {
            dto.setAssignedTechnicianName(names.computeIfAbsent(request.getAssignedTechnicianId(), this::userName));
        }

        ServiceRequestStatus status = request.getStatus();
        dto.setAllowedTransitions(TransitionPolicy.allowedFor(status, actor.actor()).stream()
                .filter(rule -> !reinspectsReplaced(request, rule.to()))
                .map(rule -> new AllowedTransitionDto(rule.to().code(),
                        rule.resolutions().stream().map(Resolution::code).toList()))
                .toList());
        boolean technical = actor.actor() == Actor.TECHNICIAN || actor.actor() == Actor.ADMIN;
        dto.setCanAddNote(technical && IN_PROGRESS.contains(status));
        dto.setCanRelinkCard(technical && status == INSPECTING);
        dto.setCanReplaceDevice(technical && status == REPAIRING);
        return dto;
    }

    private String userName(Integer userId) {
        return identityApi.getUserById(userId).map(UserDto::getFullName).orElse("Tài khoản #" + userId);
    }
}
