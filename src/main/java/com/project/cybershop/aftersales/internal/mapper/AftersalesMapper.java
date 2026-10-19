package com.project.cybershop.aftersales.internal.mapper;

import com.project.cybershop.aftersales.api.dto.response.MyServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.response.MyTimelineEntryDto;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestDto;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestEventDto;
import com.project.cybershop.aftersales.api.dto.response.ServiceRequestSummaryDto;
import com.project.cybershop.aftersales.internal.entity.Resolution;
import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestEvent;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

// Các trường ghép từ nguồn khác (thẻ, đơn hàng, tên người thao tác) do service gán sau khi map.
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AftersalesMapper {

    ServiceRequestSummaryDto toSummary(ServiceRequest request);

    ServiceRequestDto toDetail(ServiceRequest request);

    ServiceRequestEventDto toDto(ServiceRequestEvent event);

    MyServiceRequestDto toMine(ServiceRequest request);

    @Mapping(target = "status", source = "toStatus")
    MyTimelineEntryDto toTimeline(ServiceRequestEvent event);

    default String code(ServiceRequestStatus status) {
        return status == null ? null : status.code();
    }

    default String code(Resolution resolution) {
        return resolution == null ? null : resolution.code();
    }
}
