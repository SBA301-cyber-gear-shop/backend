package com.project.cybershop.order.internal.mapper;

import com.project.cybershop.order.api.dto.response.CheckoutItemResponse;
import com.project.cybershop.order.internal.repository.CheckoutItemProjection;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CheckoutMapper {

    CheckoutItemResponse toItemResponse(CheckoutItemProjection item, BigDecimal subtotal);
}
