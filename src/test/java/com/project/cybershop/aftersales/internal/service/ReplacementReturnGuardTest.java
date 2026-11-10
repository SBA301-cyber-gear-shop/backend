package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.request.RelinkCardRequest;
import com.project.cybershop.aftersales.internal.entity.*;
import com.project.cybershop.aftersales.internal.repository.*;
import com.project.cybershop.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplacementReturnGuardTest {
    private final ServiceRequestRepository requests = mock(ServiceRequestRepository.class);
    private final ServiceRequestEventRepository events = mock(ServiceRequestEventRepository.class);
    private final WarrantyCardRepository cards = mock(WarrantyCardRepository.class);
    private final ServiceRequestService service = new ServiceRequestService(requests, events, cards,
            mock(OrderLookup.class), mock(WarrantyCardService.class), mock(com.project.cybershop.aftersales.internal.mapper.AftersalesMapper.class),
            mock(com.project.cybershop.identity.api.IdentityApi.class), mock(ServiceIntakeRepository.class),
            mock(com.project.cybershop.catalog.api.CatalogInternalApi.class));

    @Test
    void blocksNewOrRelinkedRequestUntilReplacementIsReturned() {
        WarrantyCard replacement = new WarrantyCard();
        replacement.setId(2);
        replacement.setReplacesCardId(1);
        when(cards.findById(2)).thenReturn(Optional.of(replacement));
        when(requests.existsByWarrantyCardIdAndResolutionAndStatusNot(1, Resolution.REPLACED, ServiceRequestStatus.RETURNED)).thenReturn(true);
        ServiceRequest request = new ServiceRequest();
        request.setWarrantyCardId(2);
        var actor = new CurrentUser.Principal(1, "Staff", Actor.STAFF);
        assertThrows(BusinessException.class, () -> service.open(request, actor, "note", WarrantyRules.now()));
        verify(requests, never()).saveAndFlush(any());

        request.setStatus(ServiceRequestStatus.INSPECTING);
        request.setWarrantyCardId(3);
        WarrantyCard current = new WarrantyCard();
        current.setId(3);
        when(cards.findById(3)).thenReturn(Optional.of(current));
        when(requests.findById(5)).thenReturn(Optional.of(request));
        RelinkCardRequest relink = new RelinkCardRequest();
        relink.setWarrantyCardId(2);
        assertThrows(BusinessException.class, () -> service.relinkCard(5, relink, actor));

        when(requests.existsByWarrantyCardIdAndResolutionAndStatusNot(1, Resolution.REPLACED, ServiceRequestStatus.RETURNED)).thenReturn(false);
        request.setWarrantyCardId(2);
        service.open(request, actor, "note", WarrantyRules.now());
        verify(requests).saveAndFlush(request);
    }
}
