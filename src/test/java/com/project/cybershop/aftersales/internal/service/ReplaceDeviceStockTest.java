package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.aftersales.api.dto.request.ReplacementRequest;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.response.WarrantyCardDto;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestEvent;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import com.project.cybershop.aftersales.internal.mapper.AftersalesMapper;
import com.project.cybershop.aftersales.internal.repository.OrderLookup;
import com.project.cybershop.aftersales.internal.repository.OrderLookup.OrderInfo;
import com.project.cybershop.aftersales.internal.repository.ServiceIntakeRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestEventRepository;
import com.project.cybershop.aftersales.internal.repository.ServiceRequestRepository;
import com.project.cybershop.aftersales.internal.repository.WarrantyCardRepository;
import com.project.cybershop.aftersales.internal.service.CurrentUser.Principal;
import com.project.cybershop.catalog.api.CatalogInternalApi;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** T9: đổi thiết bị lấy từ hàng đang bán phải trừ đúng 1 tồn kho của phiên bản thiết bị cũ. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReplaceDeviceStockTest {
    private static final int VARIANT_ID = 77;

    @Mock ServiceRequestRepository requests;
    @Mock ServiceRequestEventRepository events;
    @Mock WarrantyCardRepository cards;
    @Mock OrderLookup orderLookup;
    @Mock WarrantyCardService cardService;
    @Mock AftersalesMapper mapper;
    @Mock IdentityApi identityApi;
    @Mock ServiceIntakeRepository intakes;
    @Mock CatalogInternalApi catalogApi;
    @InjectMocks ServiceRequestService service;

    private final Principal technician = new Principal(5, "KTV", Actor.TECHNICIAN);
    private ServiceRequest ticket;
    private WarrantyCard oldCard;

    @BeforeEach
    void setUp() {
        LocalDateTime now = WarrantyRules.now();
        oldCard = new WarrantyCard();
        oldCard.setId(10);
        oldCard.setOrderItemId(20);
        oldCard.setSerialNumber("SN-OLD");
        oldCard.setStatus(WarrantyCard.ACTIVE);
        oldCard.setStartDate(now.minusDays(1));
        oldCard.setEndDate(now.plusMonths(12));

        ticket = new ServiceRequest();
        ticket.setId(1);
        ticket.setIntakeId(2);
        ticket.setWarrantyCardId(10);
        ticket.setStatus(ServiceRequestStatus.REPAIRING);
        ticket.setCreatedAt(now);

        when(requests.findById(1)).thenReturn(Optional.of(ticket));
        when(cards.findById(10)).thenReturn(Optional.of(oldCard));
        when(orderLookup.byCardIds(List.of(10))).thenReturn(Map.of(10, new OrderInfo(10, 20, "Card", "8GB", "SKU", 12,
                30, "TZ-1", "A", "0900000000", 40, "A", "0900000000", null, VARIANT_ID)));
        when(mapper.toDetail(any())).thenAnswer(inv -> new ServiceRequestDto());
        when(cardService.toDtos(any())).thenReturn(List.of(new WarrantyCardDto()));
        when(intakes.findById(any())).thenReturn(Optional.empty());
        when(events.findByServiceRequestIdOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of());
    }

    private ReplacementRequest request(String source) {
        return ReplacementRequest.builder().newSerialNumber("sn-new").source(source)
                .internalNote("ghi chú").publicNote("thông báo").build();
    }

    @Test
    void stockSourceDeductsExactlyOneOfTheOldDevicesVariant() {
        service.replaceDevice(1, request("stock"), technician);

        verify(catalogApi).deductStock(VARIANT_ID, 1);
        assertEquals(WarrantyCard.REPLACED, oldCard.getStatus());
        ArgumentCaptor<ServiceRequestEvent> event = ArgumentCaptor.forClass(ServiceRequestEvent.class);
        verify(events).save(event.capture());
        assertTrue(event.getValue().getInternalNote().contains("đã trừ 1 tồn kho"));
    }

    @Test
    void supplierSourceDoesNotTouchStock() {
        service.replaceDevice(1, request("supplier"), technician);

        verify(catalogApi, never()).deductStock(anyInt(), anyInt());
        assertEquals(WarrantyCard.REPLACED, oldCard.getStatus());
    }

    @Test
    void outOfStockIsRejectedWithClearMessageAndNothingIsClosed() {
        doThrow(new BusinessException("Không đủ tồn kho cho phiên bản ID: 77"))
                .when(catalogApi).deductStock(VARIANT_ID, 1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replaceDevice(1, request("stock"), technician));

        assertEquals("Phiên bản này đã hết hàng trong kho. Hãy chọn nguồn hãng gửi về.", error.getMessage());
        assertEquals(WarrantyCard.ACTIVE, oldCard.getStatus());
        assertEquals(ServiceRequestStatus.REPAIRING, ticket.getStatus());
        verify(events, never()).save(any());
    }

    @Test
    void ticketReportsWhetherStockCanBeUsedOnlyWhileReplaceable() {
        when(catalogApi.isVariantInStock(VARIANT_ID, 1)).thenReturn(false);
        assertFalse(service.get(1, technician).isReplacementInStock());

        when(catalogApi.isVariantInStock(VARIANT_ID, 1)).thenReturn(true);
        assertTrue(service.get(1, technician).isReplacementInStock());

        ticket.setStatus(ServiceRequestStatus.INSPECTING);
        assertFalse(service.get(1, technician).isReplacementInStock());
    }
}
