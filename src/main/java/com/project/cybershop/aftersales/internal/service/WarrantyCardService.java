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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WarrantyCardService {

    private final WarrantyCardRepository cards;
    private final ServiceRequestRepository requests;
    private final ServiceRequestEventRepository events;
    private final ServiceIntakeRepository intakes;
    private final OrderLookup orderLookup;
    private final AftersalesMapper mapper;

    public WarrantyCardService(WarrantyCardRepository cards,
                               ServiceRequestRepository requests,
                               ServiceRequestEventRepository events,
                               ServiceIntakeRepository intakes,
                               OrderLookup orderLookup,
                               AftersalesMapper mapper) {
        this.cards = cards;
        this.requests = requests;
        this.events = events;
        this.intakes = intakes;
        this.orderLookup = orderLookup;
        this.mapper = mapper;
    }

    /** Tra cứu rồi trả cả đơn chứa thẻ khớp, để khách mang một linh kiện hay cả bộ máy đều thấy đủ đơn đã mua. */
    public List<WarrantyCardDto> search(String query) {
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
            return WarrantyCardDto.builder()
                    .id(card.getId())
                    .serialNumber(card.getSerialNumber())
                    .startDate(card.getStartDate())
                    .endDate(card.getEndDate())
                    .status(card.getStatus())
                    .valid(WarrantyRules.isValidOn(card, today))
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
                    .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count())
                    .openRequest(history.stream().filter(t -> t.getStatus().isOpen()).findFirst()
                            .map(t -> summary(t, card, order, codes)).orElse(null))
                    .requests(history.stream().map(t -> summary(t, card, order, codes)).toList())
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
        Map<Integer, String> codes = intakeCodes(list);
        return list.stream()
                .map(t -> summary(t, cardById.get(t.getWarrantyCardId()), orders.get(t.getWarrantyCardId()), codes))
                .toList();
    }

    /** Thẻ và phiếu của một khách, chỉ gồm nội dung công khai. */
    public List<MyWarrantyCardDto> myCards(Integer userId) {
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
            return MyWarrantyCardDto.builder()
                    .id(card.getId())
                    .serialNumber(card.getSerialNumber())
                    .startDate(card.getStartDate())
                    .endDate(card.getEndDate())
                    .status(card.getStatus())
                    .valid(WarrantyRules.isValidOn(card, today))
                    .productName(order == null ? null : order.productName())
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
        Map<Integer, String> codes = intakeCodes(list);
        return list.stream().collect(Collectors.toMap(ServiceRequest::getId, t -> {
            MyServiceRequestDto dto = mapper.toMine(t);
            OrderInfo order = orders.get(t.getWarrantyCardId());
            WarrantyCard card = cardById.get(t.getWarrantyCardId());
            dto.setIntakeCode(codes.get(t.getIntakeId()));
            dto.setProductName(order == null ? null : order.productName());
            dto.setSerialNumber(card == null ? null : card.getSerialNumber());
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
                                             Map<Integer, String> intakeCodes) {
        ServiceRequestSummaryDto dto = mapper.toSummary(request);
        dto.setIntakeCode(intakeCodes.get(request.getIntakeId()));
        dto.setSerialNumber(card == null ? null : card.getSerialNumber());
        dto.setProductName(order == null ? null : order.productName());
        dto.setOrderCode(order == null ? null : order.orderCode());
        return dto;
    }
}
