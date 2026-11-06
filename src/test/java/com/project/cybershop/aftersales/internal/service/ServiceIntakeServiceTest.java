package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.request.IntakeCreateRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceIntake;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.OrderLookup.OrderInfo;
import com.project.cybershop.aftersales.internal.repository.ServiceIntakeRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServiceIntakeServiceTest {
    private final ServiceIntakeRepository intakes = mock(ServiceIntakeRepository.class);
    private final ServiceRequestRepository requests = mock(ServiceRequestRepository.class);
    private final ServiceRequestService requestService = mock(ServiceRequestService.class);
    private final WarrantyCardService cardService = mock(WarrantyCardService.class);
    private final OrderLookup orderLookup = mock(OrderLookup.class);
    private final ServiceIntakeService service = new ServiceIntakeService(intakes, requests, requestService, cardService, orderLookup);
    private final CurrentUser.Principal staff = new CurrentUser.Principal(9, "Staff", Actor.STAFF);

    private IntakeCreateRequest request(List<Integer> valid, List<Integer> expired) {
        return IntakeCreateRequest.builder().warrantyCardIds(valid).outOfWarrantyCardIds(expired)
                .issueDescription("Máy không chạy").conditionNote("Nguyên vẹn").accessoriesNote("Không")
                .contactName("Khách").contactPhone("0900000000").expectedReturnDate(WarrantyRules.now().toLocalDate().plusDays(2)).build();
    }

    private WarrantyCard card(int id, LocalDate end) {
        WarrantyCard card = new WarrantyCard();
        card.setId(id);
        card.setSerialNumber("SERIAL-" + id);
        card.setStartDate(end.minusYears(1).atStartOfDay());
        card.setEndDate(end.atStartOfDay());
        return card;
    }

    private OrderInfo order(int cardId, int orderId) {
        return new OrderInfo(cardId, 20 + cardId, "Linh kiện", "", "SKU", 12, orderId,
                "ORDER", "Khách", "0900000000", 7, "Khách", "0900000000", null, cardId);
    }

    @Test
    void acceptsOnlyExpiredComponentsAndCreatesIndependentRequests() {
        LocalDate today = WarrantyRules.now().toLocalDate();
        WarrantyCard expired = card(2, today.minusDays(1));
        List<ServiceRequest> opened = new ArrayList<>();
        when(requestService.card(2)).thenReturn(expired);
        when(orderLookup.byCardIds(any())).thenReturn(Map.of(2, order(2, 30)));
        when(intakes.saveAndFlush(any())).thenAnswer(inv -> {
            ServiceIntake intake = inv.getArgument(0);
            intake.setId(42);
            return intake;
        });
        doAnswer(inv -> { opened.add(inv.getArgument(0)); return null; })
                .when(requestService).open(any(), any(), anyString(), any());
        when(requests.findByIntakeIdOrderByIdAsc(42)).thenAnswer(inv -> opened);

        var result = service.create(request(List.of(), List.of(2)), staff);

        assertEquals(30, result.getOrderId());
        assertEquals(1, opened.size());
        assertEquals(2, opened.get(0).getWarrantyCardId());
        assertEquals(ServiceRequestStatus.RECEIVED, opened.get(0).getStatus());
        assertTrue(opened.get(0).getPublicNote().contains("ngoài bảo hành"));
        assertTrue(opened.get(0).getRequestCode().endsWith("-01"));
    }

    @Test
    void acceptsMixedComponentsAndKeepsEachSerialOnItsOwnRequest() {
        LocalDate today = WarrantyRules.now().toLocalDate();
        WarrantyCard valid = card(1, today.plusDays(30));
        WarrantyCard expired = card(2, today.minusDays(1));
        List<ServiceRequest> opened = new ArrayList<>();
        when(requestService.card(1)).thenReturn(valid);
        when(requestService.card(2)).thenReturn(expired);
        when(orderLookup.byCardIds(any())).thenReturn(Map.of(1, order(1, 30), 2, order(2, 30)));
        when(intakes.saveAndFlush(any())).thenAnswer(inv -> {
            ServiceIntake intake = inv.getArgument(0);
            intake.setId(42);
            return intake;
        });
        doAnswer(inv -> { opened.add(inv.getArgument(0)); return null; })
                .when(requestService).open(any(), any(), anyString(), any());
        when(requests.findByIntakeIdOrderByIdAsc(42)).thenAnswer(inv -> opened);

        service.create(request(List.of(1), List.of(2)), staff);

        assertEquals(List.of(1, 2), opened.stream().map(ServiceRequest::getWarrantyCardId).toList());
        assertNotEquals(opened.get(0).getRequestCode(), opened.get(1).getRequestCode());
        assertTrue(opened.get(1).getPublicNote().contains("ngoài bảo hành"));
    }

    @Test
    void rejectsEmptySelectionAndCardsFromDifferentOrders() {
        assertThrows(BusinessException.class, () -> service.create(request(List.of(), List.of()), staff));
        when(orderLookup.byCardIds(any())).thenReturn(Map.of(1, order(1, 30), 2, order(2, 31)));
        assertThrows(BusinessException.class, () -> service.create(request(List.of(1), List.of(2)), staff));
        verify(intakes, never()).saveAndFlush(any());
    }
}
