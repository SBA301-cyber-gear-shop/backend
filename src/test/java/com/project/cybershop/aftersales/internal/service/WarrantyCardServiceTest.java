package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.response.ServiceRequestSummaryDto;
import com.project.cybershop.aftersales.api.dto.response.WarrantyCardDto;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.mapper.AftersalesMapper;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.ServiceIntakeRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestEventRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.aftersales.internal.repository.WarrantyCardRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarrantyCardServiceTest {
    @Test
    void cardSearchShowsRemainingTimeForActiveExpiredAndOpenRequests() {
        WarrantyCardRepository cards = mock(WarrantyCardRepository.class);
        ServiceRequestRepository requests = mock(ServiceRequestRepository.class);
        OrderLookup orders = mock(OrderLookup.class);
        AftersalesMapper mapper = mock(AftersalesMapper.class);
        WarrantyCardService service = new WarrantyCardService(cards, requests,
                mock(ServiceRequestEventRepository.class), mock(ServiceIntakeRepository.class), orders, mapper);
        LocalDate today = WarrantyRules.now().toLocalDate();
        WarrantyCard card = new WarrantyCard();
        card.setId(1);
        card.setStartDate(today.minusMonths(6).atStartOfDay());
        card.setEndDate(today.plusDays(20).atStartOfDay());
        when(orders.byCardIds(any())).thenReturn(Map.of());
        when(mapper.toSummary(any())).thenAnswer(invocation -> new ServiceRequestSummaryDto());

        WarrantyCardDto active = service.toDtos(List.of(card)).get(0);
        assertEquals(today, active.getWarrantyRemainingAsOf());
        assertEquals(20L, active.getWarrantyRemainingDays());
        assertTrue(active.isValid());

        card.setEndDate(today.minusDays(2).atStartOfDay());
        WarrantyCardDto expired = service.toDtos(List.of(card)).get(0);
        assertEquals(0L, expired.getWarrantyRemainingDays());
        assertFalse(expired.isValid());

        ServiceRequest request = new ServiceRequest();
        request.setIntakeId(2);
        request.setWarrantyCardId(1);
        request.setStatus(ServiceRequestStatus.REPAIRING);
        request.setCreatedAt(today.minusDays(5).atStartOfDay());
        when(requests.findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(any())).thenReturn(List.of(request));
        WarrantyCardDto inService = service.toDtos(List.of(card)).get(0);
        assertEquals(today.minusDays(5), inService.getWarrantyRemainingAsOf());
        assertEquals(3L, inService.getWarrantyRemainingDays());

        card.setStatus(WarrantyCard.REPLACED);
        assertNull(service.toDtos(List.of(card)).get(0).getWarrantyRemainingDays());
    }

    @Test
    void summaryShowsRemainingAtIntakeWhileOpenAndAtTodayAfterReturn() {
        WarrantyCardRepository cards = mock(WarrantyCardRepository.class);
        OrderLookup orders = mock(OrderLookup.class);
        AftersalesMapper mapper = mock(AftersalesMapper.class);
        WarrantyCardService service = new WarrantyCardService(cards, mock(ServiceRequestRepository.class),
                mock(ServiceRequestEventRepository.class), mock(ServiceIntakeRepository.class), orders, mapper);
        LocalDate today = WarrantyRules.now().toLocalDate();
        WarrantyCard card = new WarrantyCard();
        card.setId(1);
        card.setStartDate(today.minusMonths(6).atStartOfDay());
        card.setEndDate(today.plusDays(20).atStartOfDay());
        ServiceRequest request = new ServiceRequest();
        request.setIntakeId(2);
        request.setWarrantyCardId(1);
        request.setCreatedAt(today.minusDays(5).atStartOfDay());
        request.setStatus(ServiceRequestStatus.REPAIRING);
        when(cards.findAllById(any())).thenReturn(List.of(card));
        when(orders.byCardIds(any())).thenReturn(Map.of());
        when(mapper.toSummary(request)).thenAnswer(invocation -> new ServiceRequestSummaryDto());

        ServiceRequestSummaryDto open = service.summaries(List.of(request)).get(0);
        assertEquals(today.minusDays(5), open.getWarrantyRemainingAsOf());
        assertEquals(25L, open.getWarrantyRemainingDays());
        assertEquals(card.getEndDate(), open.getWarrantyEndDate());

        request.setStatus(ServiceRequestStatus.READY_FOR_RETURN);
        request.setCompletedAt(today.minusDays(2).atStartOfDay());
        request.setResolution(Resolution.REPAIRED);
        ServiceRequestSummaryDto ready = service.summaries(List.of(request)).get(0);
        assertEquals(today, ready.getWarrantyRemainingAsOf());
        assertEquals(card.getEndDate().plusDays(3), ready.getWarrantyEndDate());
        assertEquals(23L, ready.getWarrantyRemainingDays());

        request.setResolution(Resolution.NO_FAULT_FOUND);
        assertEquals(20L, service.summaries(List.of(request)).get(0).getWarrantyRemainingDays());

        request.setStatus(ServiceRequestStatus.RETURNED);
        request.setResolution(Resolution.REPAIRED);
        card.setEndDate(card.getEndDate().plusDays(5));
        ServiceRequestSummaryDto returned = service.summaries(List.of(request)).get(0);
        assertEquals(today, returned.getWarrantyRemainingAsOf());
        assertEquals(25L, returned.getWarrantyRemainingDays());

        card.setStatus(WarrantyCard.REPLACED);
        assertNull(service.summaries(List.of(request)).get(0).getWarrantyRemainingDays());
    }
}
