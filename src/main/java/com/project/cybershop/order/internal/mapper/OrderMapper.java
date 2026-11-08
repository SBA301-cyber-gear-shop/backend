package com.project.cybershop.order.internal.mapper;

import com.project.cybershop.catalog.api.dto.response.PurchaseVariantResponse;
import com.project.cybershop.order.api.dto.response.CheckoutDetailsResponse;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import com.project.cybershop.order.internal.entity.Order;
import com.project.cybershop.order.internal.entity.OrderItem;
import com.project.cybershop.order.internal.repository.OrderItemProjection;
import com.project.cybershop.payment.api.PaymentApi;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "pending")
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "finalAmount", source = "finalAmount")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "createdAt")
    @Mapping(target = "deliveryType", ignore = true)
    @Mapping(target = "shippingFee", ignore = true)
    @Mapping(target = "shippingAddress", ignore = true)
    @Mapping(target = "toProvinceId", ignore = true)
    @Mapping(target = "toDistrictId", ignore = true)
    @Mapping(target = "toWardCode", ignore = true)
    @Mapping(target = "trackingCode", ignore = true)
    @Mapping(target = "carrier", ignore = true)
    Order toOrder(
            CheckoutDetailsResponse checkout,
            Integer userId,
            String orderCode,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            LocalDateTime createdAt
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productVariantId", source = "product.variantId")
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "productName", source = "productName")
    OrderItem toOrderItem(
            PurchaseVariantResponse product,
            Integer orderId,
            Integer quantity,
            String productName,
            BigDecimal subtotal
    );

    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "paymentStatus", source = "payment.status")
    @Mapping(target = "paymentUrl", source = "paymentUrl")
    @Mapping(target = "deliveryType", source = "order.deliveryType")
    @Mapping(target = "shippingFee", source = "order.shippingFee")
    @Mapping(target = "shippingAddress", source = "order.shippingAddress")
    @Mapping(target = "trackingCode", source = "order.trackingCode")
    @Mapping(target = "carrier", source = "order.carrier")
    OrderResponse toResponse(
            Order order,
            PaymentApi.Payment payment,
            String promotionCode,
            List<OrderResponse.Item> items,
            String paymentUrl
    );

    default OrderResponse toResponse(
            Order order,
            PaymentApi.Payment payment,
            String promotionCode,
            List<OrderResponse.Item> items
    ) {
        return toResponse(order, payment, promotionCode, items, null);
    }

    OrderResponse.Item toItemResponse(OrderItemProjection item);
}
