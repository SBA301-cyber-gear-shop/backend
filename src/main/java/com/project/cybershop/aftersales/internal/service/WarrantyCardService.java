package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.MyServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.MyWarrantyCardDto;
import com.project.cybershop.aftersales.api.dto.ServiceRequestSummaryDto;
import com.project.cybershop.aftersales.api.dto.WarrantyCardDto;
import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestEvent;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.OrderLookup.OrderInfo;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestEventRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.aftersales.internal.repository.WarrantyCardRepository;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WarrantyCardService {

    private final WarrantyCardRepository cards;
    private final ServiceRequestRepository requests;
    private final ServiceRequestEventRepository events;
    private final OrderLookup orderLookup;
    private final AftersalesMapper mapper;

    public WarrantyCardService(WarrantyCardRepository cards,
                               ServiceRequestRepository requests,
                               ServiceRequestEventRepository events,
                               OrderLookup orderLookup,
                               AftersalesMapper mapper) {
        this.cards = cards;
        this.requests = requests;
        this.events = events;
        this.orderLookup = orderLookup;
        this.mapper = mapper;
    }

    public List<WarrantyCardDto> search(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("Nhập serial, mã đơn, số điện thoại hoặc mã phiếu.");
        }
        List<WarrantyCard> found = cards.findAllById(orderLookup.searchCardIds(query));
        return toDtos(found.stream().sorted(Comparator.comparing(WarrantyCard::getId)).toList());
    }

    /** Thẻ kèm thông tin đơn hàng, phiếu đang mở và các phiếu cũ. */
    public List<WarrantyCardDto> toDtos(List<WarrantyCard> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = list.stream().map(WarrantyCard::getId).toList();
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(ids);
        Map<Integer, List<ServiceRequest>> byCard = requestsByCard(ids);
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
                    .customerName(order == null ? null : order.customerName())
                    .customerPhone(order == null ? null : order.customerPhone())
                    .pickupName(order == null ? null : order.pickupName())
                    .pickupPhone(order == null ? null : order.pickupPhone())
                    .replacesCardId(card.getReplacesCardId())
                    .replacedByCardId(next == null ? null : next.getId())
                    .replacedBySerialNumber(next == null ? null : next.getSerialNumber())
                    .repairedCount(history.stream().filter(t -> t.getResolution() == Resolution.REPAIRED).count())
                    .openRequest(history.stream().filter(t -> t.getStatus().isOpen()).findFirst()
                            .map(t -> summary(t, card, order)).orElse(null))
                    .requests(history.stream().map(t -> summary(t, card, order)).toList())
                    .build();
        }).toList();
    }

    /** Phiếu kèm serial và tên sản phẩm, dùng cho danh sách. */
    public List<ServiceRequestSummaryDto> summaries(List<ServiceRequest> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Integer> cardIds = list.stream().map(ServiceRequest::getWarrantyCardId).distinct().toList();
        Map<Integer, WarrantyCard> cardById = cards.findAllById(cardIds).stream()
                .collect(Collectors.toMap(WarrantyCard::getId, Function.identity()));
        Map<Integer, OrderInfo> orders = orderLookup.byCardIds(cardIds);
        return list.stream()
                .map(t -> summary(t, cardById.get(t.getWarrantyCardId()), orders.get(t.getWarrantyCardId())))
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
        List<Integer> requestIds = byCard.values().stream().flatMap(Collection::stream).map(ServiceRequest::getId).toList();
        Map<Integer, List<ServiceRequestEvent>> timeline = requestIds.isEmpty() ? Map.of()
                : events.findByServiceRequestIdInOrderByCreatedAtAscIdAsc(requestIds).stream()
                        .collect(Collectors.groupingBy(ServiceRequestEvent::getServiceRequestId));
        LocalDate today = WarrantyRules.now().toLocalDate();

        return mine.stream().map(card -> {
            OrderInfo order = orders.get(card.getId());
            WarrantyCard next = replacedBy.get(card.getId());
            List<MyServiceRequestDto> mineRequests = byCard.getOrDefault(card.getId(), List.of()).stream()
                    .filter(t -> Objects.equals(t.getUserId(), userId))
                    .map(t -> {
                        MyServiceRequestDto dto = mapper.toMine(t);
                        // Dòng chỉ có ghi chú nội bộ không có gì để khách xem.
                        dto.setTimeline(timeline.getOrDefault(t.getId(), List.of()).stream()
                                .filter(e -> e.getFromStatus() != e.getToStatus() || e.getPublicNote() != null)
                                .map(mapper::toTimeline)
                                .toList());
                        return dto;
                    })
                    .toList();
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
                    .requests(mineRequests)
                    .build();
        }).toList();
    }

    private Map<Integer, List<ServiceRequest>> requestsByCard(List<Integer> cardIds) {
        return requests.findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(cardIds).stream()
                .collect(Collectors.groupingBy(ServiceRequest::getWarrantyCardId));
    }

    private Map<Integer, WarrantyCard> replacedBy(List<Integer> cardIds) {
        return cards.findByReplacesCardIdIn(cardIds).stream()
                .collect(Collectors.toMap(WarrantyCard::getReplacesCardId, Function.identity()));
    }

    private ServiceRequestSummaryDto summary(ServiceRequest request, WarrantyCard card, OrderInfo order) {
        ServiceRequestSummaryDto dto = mapper.toSummary(request);
        dto.setSerialNumber(card == null ? null : card.getSerialNumber());
        dto.setProductName(order == null ? null : order.productName());
        return dto;
    }
}
