package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.response.MyServiceIntakeDto;
import com.project.cybershop.aftersales.api.dto.response.MyServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.response.MyWarrantyCardDto;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestSummaryDto;
import com.project.cybershop.aftersales.api.dto.response.WarrantyCardDto;
import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceIntake;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestEvent;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.OrderLookup.OrderInfo;
import com.project.cybershop.aftersales.internal.repository.ServiceIntakeRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestEventRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.aftersales.internal.repository.WarrantyCardRepository;
import com.project.cybershop.aftersales.internal.mapper.AftersalesMapper;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class WarrantyCardService {

    private final WarrantyCardRepository cards;
    private final ServiceRequestRepository requests;
    private final ServiceRequestEventRepository events;
    private final ServiceIntakeRepository intakes;
        private final OrderLookup orderLookup;
    private final AftersalesMapper mapper;
    private final JdbcTemplate jdbc;

    @Autowired
    public WarrantyCardService(WarrantyCardRepository cards,
                               ServiceRequestRepository requests,
                               ServiceRequestEventRepository events,
                               ServiceIntakeRepository intakes,
                               OrderLookup orderLookup,
                               AftersalesMapper mapper,
                               JdbcTemplate jdbc) {
        this.cards = cards;
        this.requests = requests;
        this.events = events;
        this.intakes = intakes;
        this.orderLookup = orderLookup;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    public WarrantyCardService(WarrantyCardRepository cards, ServiceRequestRepository requests, ServiceRequestEventRepository events, ServiceIntakeRepository intakes, OrderLookup orderLookup, AftersalesMapper mapper) {
        this(cards, requests, events, intakes, orderLookup, mapper, null);
    }

    @Transactional
    public void syncMissingCards() {
        if (jdbc == null) return;
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT oi.id, oi.quantity, oi.warranty_period_months, o.order_code, o.updated_at, o.delivery_type
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    WHERE o.status = 'completed'
                      AND oi.warranty_period_months > 0
                      AND NOT EXISTS (SELECT 1 FROM warranty_cards wc WHERE wc.order_item_id = oi.id)
                    """);

            for (Map<String, Object> r : rows) {
                Integer orderItemId = ((Number) r.get("id")).intValue();
                int qty = ((Number) r.get("quantity")).intValue();
                int months = ((Number) r.get("warranty_period_months")).intValue();
                String orderCode = (String) r.get("order_code");
                Object updatedObj = r.get("updated_at");
                LocalDateTime start = updatedObj instanceof java.sql.Timestamp ts ? ts.toLocalDateTime()
                        : updatedObj instanceof LocalDateTime ldt ? ldt : LocalDateTime.now();
                String deliveryType = (String) r.get("delivery_type");
                String prefix = "shipping".equalsIgnoreCase(deliveryType) ? "GHN" : "TZ";
                String suffix = (orderCode != null && orderCode.length() >= 6)
                        ? orderCode.substring(orderCode.length() - 6).toUpperCase(Locale.ROOT)
                        : "DEV";

                for (int i = 0; i < qty; i++) {
                    String serial = "SN-" + prefix + "-" + suffix + "-" + orderItemId + (qty > 1 ? "-" + (i + 1) : "");
                    jdbc.update("""
                            INSERT INTO warranty_cards (order_item_id, serial_number, start_date, end_date, status)
                            VALUES (?, ?, ?, ?, 'active')
                            """, orderItemId, serial, start, start.plusMonths(months));
                }
            }
        } catch (Exception e) {
            System.err.println("syncMissingCards error: " + e.getMessage());
        }
    }

    /** Tra cứu rồi trả cả đơn chứa thẻ khớp, để khách mang một linh kiện hay cả bộ máy đều thấy đủ đơn đã mua. */
    @Transactional
    public List<WarrantyCardDto> search(String query) {
        syncMissingCards();
        if (query == null || query.isBlank()) {
            throw new BusinessException("Nhập serial, mã đơn, số điện thoại hoặc mã phiếu.");
        }
        Set<Integer> matched = new HashSet<>(orderLookup.searchCardIds(query));
        Set<Integer> orderIds = orderLookup.byCardIds(matched).values().stream()
                .map(OrderInfo::orderId).collect(Collectors.toSet());
        List<WarrantyCard> found = cards.findAllById(orderLookup.cardIdsOfOrders(orderIds));
        return toDtos(found.stream().sorted(Comparator.comparing(WarrantyCard::getId)).toList(), matched);
    }

    public List<WarrantyCardDto> toDtos(List<WarrantyCard> list) {
        return toDtos(list, Set.of());
    }

    /** Thẻ kèm thông tin đơn hàng, phiếu đang mở và các phiếu cũ. */
    private List<WarrantyCardDto> toDtos(List<WarrantyCard> list, Set<Integer> matched) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = list.stream().map(WarrantyCard::getId).toList();
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(ids);
        Map<Integer, List<ServiceRequest>> byCard = requestsByCard(ids);
        Map<Integer, String> codes = intakeCodes(byCard.values().stream().flatMap(Collection::stream).toList());
        Map<Integer, WarrantyCard> replacedBy = replacedBy(ids);
        LocalDate today = WarrantyRules.now().toLocalDate();

        return list.stream().map(card -> {
            OrderInfo order = orders.get(card.getId());
            List<ServiceRequest> history = byCard.getOrDefault(card.getId(), List.of());
            WarrantyCard next = replacedBy.get(card.getId());
            ServiceRequest open = history.stream().filter(t -> t.getStatus().isOpen()).findFirst().orElse(null);
            LocalDate asOf = open == null ? today : remainingAsOf(open, card);
            Long days = !WarrantyCard.ACTIVE.equals(card.getStatus()) || asOf == null ? null
                    : open == null ? Math.max(0, ChronoUnit.DAYS.between(asOf, card.getEndDate().toLocalDate()))
                    : remainingDays(open, card, asOf);
            LocalDateTime displayEnd = history.stream().filter(t -> t.getStatus() == ServiceRequestStatus.READY_FOR_RETURN
                            && t.getResolution() == Resolution.REPAIRED)
                    .map(t -> effectiveWarrantyEnd(t, card)).findFirst().orElse(card.getEndDate());
            return WarrantyCardDto.builder()
                    .id(card.getId())
                    .orderItemId(card.getOrderItemId())
                    .serialNumber(card.getSerialNumber())
                    .startDate(card.getStartDate())
                    .endDate(displayEnd)
                    .warrantyRemainingAsOf(WarrantyCard.ACTIVE.equals(card.getStatus()) ? asOf : null)
                    .warrantyRemainingDays(days)
                    .status(card.getStatus())
                    .valid(WarrantyCard.ACTIVE.equals(card.getStatus()) && !today.isBefore(card.getStartDate().toLocalDate())
                            && !today.isAfter(displayEnd.toLocalDate()))
                    .productName(order == null ? null : order.productName())
                    .variantName(order == null ? null : order.variantName())
                    .sku(order == null ? null : order.sku())
                    .orderCode(order == null ? null : order.orderCode())
                    .orderId(order == null ? null : order.orderId())
                    .pcCustomId(order == null ? null : order.pcCustomId())
                    .matched(matched.contains(card.getId()))
                    .customerName(order == null ? null : order.customerName())
                    .customerPhone(order == null ? null : order.customerPhone())
                    .pickupName(order == null ? null : order.pickupName())
                    .pickupPhone(order == null ? null : order.pickupPhone())
                    .replacesCardId(card.getReplacesCardId())
                    .replacedByCardId(next == null ? null : next.getId())
                    .replacedBySerialNumber(next == null ? null : next.getSerialNumber())
                    .replacedByEndDate(next == null ? null : next.getEndDate())
                    .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count())
                    .openRequest(open == null ? null : summary(open, card, order, codes, replacedBy))
                    .requests(history.stream().map(t -> summary(t, card, order, codes, replacedBy)).toList())
                    .build();
        }).toList();
    }

    /** Phiếu kèm serial, tên sản phẩm và mã lần tiếp nhận, dùng cho danh sách. */
    public List<ServiceRequestSummaryDto> summaries(List<ServiceRequest> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Integer> cardIds = list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList();
        Map<Integer, WarrantyCard> cardById = cards.findAllById(cardIds).stream()
                .collect(Collectors.toMap(WarrantyCard::getId, Function.identity()));
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(cardIds);
        Map<Integer, WarrantyCard> replacements = replacedBy(cardIds);
        Map<Integer, String> codes = intakeCodes(list);
        return list.stream()
                .map(t -> summary(t, cardById.get(t.getWarrantyCardId()), orders.get(t.getWarrantyCardId()), codes, replacements))
                .toList();
    }

    /** Thẻ và phiếu của một khách, chỉ gồm nội dung công khai. */
    @Transactional
    public List<MyWarrantyCardDto> myCards(Integer userId) {
        syncMissingCards();
        List<WarrantyCard> mine = cards.findAllById(orderLookup.cardIdsOfUser(userId)).stream()
                .sorted(Comparator.comparing(WarrantyCard::getId).reversed())
                .toList();
        if (mine.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = mine.stream().map(WarrantyCard::getId).toList();
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(ids);
        Map<Integer, List<ServiceRequest>> byCard = requestsByCard(ids);
        Map<Integer, WarrantyCard> replacedBy = replacedBy(ids);
        Map<Integer, MyServiceRequestDto> mineRequests = myRequests(
                byCard.values().stream().flatMap(Collection::stream)
                        .filter(t -> Objects.equals(t.getUserId(), userId)).toList(), orders);
        LocalDate today = WarrantyRules.now().toLocalDate();

        return mine.stream().map(card -> {
            OrderInfo order = orders.get(card.getId());
            WarrantyCard next = replacedBy.get(card.getId());
            LocalDateTime displayEnd = byCard.getOrDefault(card.getId(), List.of()).stream()
                    .filter(t -> t.getStatus() == ServiceRequestStatus.READY_FOR_RETURN && t.getResolution() == Resolution.REPAIRED)
                    .map(t -> effectiveWarrantyEnd(t, card)).findFirst().orElse(card.getEndDate());
            return MyWarrantyCardDto.builder()
                    .id(card.getId())
                    .serialNumber(card.getSerialNumber())
                    .startDate(card.getStartDate())
                    .endDate(displayEnd)
                    .status(card.getStatus())
                    .valid(WarrantyCard.ACTIVE.equals(card.getStatus()) && !today.isBefore(card.getStartDate().toLocalDate())
                            && !today.isAfter(displayEnd.toLocalDate()))
                    .productName(order == null ? null : order.productName())
                    .pcCustomId(order == null ? null : order.pcCustomId())
                    .variantName(order == null ? null : order.variantName())
                    .orderCode(order == null ? null : order.orderCode())
                    .replacedBySerialNumber(next == null ? null : next.getSerialNumber())
                    .requests(byCard.getOrDefault(card.getId(), List.of()).stream()
                            .map(t -> mineRequests.get(t.getId())).filter(Objects::nonNull).toList())
                    .build();
        }).toList();
    }

    /** Các lần gửi bảo hành của một khách: mỗi lần là cả bộ máy mang tới, kèm kết quả từng linh kiện. */
    public List<MyServiceIntakeDto> myIntakes(Integer userId) {
        List<ServiceIntake> mine = intakes.findByUserIdOrderByCreatedAtDescIdDesc(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<ServiceRequest>> byIntake = requests
                .findByIntakeIdInOrderByIdAsc(mine.stream().map(ServiceIntake::getId).toList()).stream()
                .collect(Collectors.groupingBy(ServiceRequest::getIntakeId));
        List<ServiceRequest> all = byIntake.values().stream().flatMap(Collection::stream).toList();
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(
                all.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList());
        Map<Integer, MyServiceRequestDto> dtos = myRequests(all, orders);

        return mine.stream().filter(i -> byIntake.containsKey(i.getId())).map(intake -> {
            List<ServiceRequest> parts = byIntake.get(intake.getId());
            ServiceRequest first = parts.get(0);
            OrderInfo order = orders.get(first.getWarrantyCardId());
            return MyServiceIntakeDto.builder()
                    .intakeCode(intake.getIntakeCode())
                    .orderCode(order == null ? null : order.orderCode())
                    .status(WarrantyRules.intakeStatus(parts.stream().map(ServiceRequest::getStatus).toList()).code())
                    .issueDescription(first.getIssueDescription())
                    .expectedReturnDate(parts.stream().map(ServiceRequest::getExpectedReturnDate)
                            .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null))
                    .createdAt(intake.getCreatedAt())
                    .returnedAt(intake.getReturnedAt())
                    .components(parts.stream().map(t -> dtos.get(t.getId())).toList())
                    .build();
        }).toList();
    }

    /** Phiếu dưới góc nhìn khách: tiến trình chỉ gồm các dòng có nội dung công khai. */
    private Map<Integer, MyServiceRequestDto> myRequests(List<ServiceRequest> list, Map<Integer, OrderInfo> orders) {
        if (list.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<ServiceRequestEvent>> timeline = events
                .findByServiceRequestIdInOrderByCreatedAtAscIdAsc(list.stream().map(ServiceRequest::getId).toList())
                .stream().collect(Collectors.groupingBy(ServiceRequestEvent::getServiceRequestId));
        Map<Integer, WarrantyCard> cardById = cards.findAllById(
                        list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList()).stream()
                .collect(Collectors.toMap(WarrantyCard::getId, Function.identity()));
        Map<Integer, WarrantyCard> replacements = replacedBy(cardById.keySet().stream().toList());
        Map<Integer, String> codes = intakeCodes(list);
        return list.stream().collect(Collectors.toMap(ServiceRequest::getId, t -> {
            MyServiceRequestDto dto = mapper.toMine(t);
            OrderInfo order = orders.get(t.getWarrantyCardId());
            WarrantyCard card = cardById.get(t.getWarrantyCardId());
            dto.setIntakeCode(codes.get(t.getIntakeId()));
            dto.setProductName(order == null ? null : order.productName());
            dto.setPcCustomId(order == null ? null : order.pcCustomId());
            dto.setSerialNumber(card == null ? null : card.getSerialNumber());
            dto.setUnderWarrantyAtIntake(WarrantyRules.wasCoveredOn(card, ServiceRequestService.receivedDay(t)));
            dto.setWarrantyStartDate(card == null ? null : card.getStartDate());
            dto.setWarrantyEndDate(effectiveWarrantyEnd(t, card));
            LocalDate asOf = remainingAsOf(t, card);
            dto.setWarrantyRemainingAsOf(asOf);
            dto.setWarrantyRemainingDays(remainingDays(t, card, asOf));
            WarrantyCard replacement = replacements.get(t.getWarrantyCardId());
            dto.setReplacementSerialNumber(replacement == null ? null : replacement.getSerialNumber());
            dto.setReplacementWarrantyEndDate(replacement == null ? null : replacement.getEndDate());
            dto.setReplacementWarrantyRemainingDays(replacementRemainingDays(replacement));
            // Dòng chỉ có ghi chú nội bộ không có gì để khách xem.
            dto.setTimeline(timeline.getOrDefault(t.getId(), List.of()).stream()
                    .filter(e -> e.getFromStatus() != e.getToStatus() || e.getPublicNote() != null)
                    .map(mapper::toTimeline)
                    .toList());
            return dto;
        }));
    }

    private Map<Integer, List<ServiceRequest>> requestsByCard(List<Integer> cardIds) {
        return requests.findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(cardIds).stream()
                .collect(Collectors.groupingBy(ServiceRequest::getWarrantyCardId));
    }

    private Map<Integer, WarrantyCard> replacedBy(List<Integer> cardIds) {
        return cards.findByReplacesCardIdIn(cardIds).stream()
                .collect(Collectors.toMap(WarrantyCard::getReplacesCardId, Function.identity()));
    }

    private Map<Integer, String> intakeCodes(Collection<ServiceRequest> list) {
        List<Integer> ids = list.stream().map(ServiceRequest::getIntakeId).filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : intakes.findAllById(ids).stream()
                .collect(Collectors.toMap(ServiceIntake::getId, ServiceIntake::getIntakeCode));
    }

    private ServiceRequestSummaryDto summary(ServiceRequest request, WarrantyCard card, OrderInfo order,
                                             Map<Integer, String> intakeCodes, Map<Integer, WarrantyCard> replacements) {
        ServiceRequestSummaryDto dto = mapper.toSummary(request);
        dto.setIntakeCode(intakeCodes.get(request.getIntakeId()));
        dto.setSerialNumber(card == null ? null : card.getSerialNumber());
        dto.setUnderWarrantyAtIntake(WarrantyRules.wasCoveredOn(card, ServiceRequestService.receivedDay(request)));
        dto.setWarrantyStartDate(card == null ? null : card.getStartDate());
        dto.setWarrantyEndDate(effectiveWarrantyEnd(request, card));
        LocalDate asOf = remainingAsOf(request, card);
        dto.setWarrantyRemainingAsOf(asOf);
        dto.setWarrantyRemainingDays(remainingDays(request, card, asOf));
        WarrantyCard replacement = replacements.get(request.getWarrantyCardId());
        dto.setReplacementSerialNumber(replacement == null ? null : replacement.getSerialNumber());
        dto.setReplacementWarrantyEndDate(replacement == null ? null : replacement.getEndDate());
        dto.setReplacementWarrantyRemainingDays(replacementRemainingDays(replacement));
        dto.setProductName(order == null ? null : order.productName());
        dto.setPcCustomId(order == null ? null : order.pcCustomId());
        dto.setOrderCode(order == null ? null : order.orderCode());
        return dto;
    }

    private LocalDate remainingAsOf(ServiceRequest request, WarrantyCard card) {
        if (card == null || !WarrantyCard.ACTIVE.equals(card.getStatus())) return null;
        return request.getStatus().isOpen() && request.getStatus() != ServiceRequestStatus.READY_FOR_RETURN
                && request.getCreatedAt() != null
                ? request.getCreatedAt().toLocalDate() : WarrantyRules.now().toLocalDate();
    }

    private LocalDateTime effectiveWarrantyEnd(ServiceRequest request, WarrantyCard card) {
        if (card == null || card.getEndDate() == null) return null;
        if (request.getStatus() == ServiceRequestStatus.READY_FOR_RETURN
                && request.getResolution() == Resolution.REPAIRED) {
            return card.getEndDate().plusDays(WarrantyRules.extensionDays(request.getCreatedAt(), request.getCompletedAt()));
        }
        return card.getEndDate();
    }

    private Long remainingDays(ServiceRequest request, WarrantyCard card, LocalDate asOf) {
        var end = effectiveWarrantyEnd(request, card);
        return asOf == null || end == null ? null : Math.max(0, ChronoUnit.DAYS.between(asOf, end.toLocalDate()));
    }

    private Long replacementRemainingDays(WarrantyCard card) {
        if (card == null || !WarrantyCard.ACTIVE.equals(card.getStatus()) || card.getEndDate() == null) return null;
        return Math.max(0, ChronoUnit.DAYS.between(WarrantyRules.now().toLocalDate(), card.getEndDate().toLocalDate()));
    }
}
