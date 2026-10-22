package com.project.cybershop.promotion.internal.mapper;

import com.project.cybershop.promotion.api.dto.request.PromotionRequest;
import com.project.cybershop.promotion.api.dto.response.PromotionResponse;
import com.project.cybershop.promotion.internal.entity.Banner;
import com.project.cybershop.promotion.internal.entity.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PromotionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", expression = "java(request.code().trim().toUpperCase(java.util.Locale.ROOT))")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "discountType", source = "type")
    @Mapping(target = "discountValue", source = "value")
    @Mapping(target = "usageLimit", source = "limit")
    @Mapping(target = "startDate", source = "start")
    @Mapping(target = "endDate", source = "end")
    @Mapping(target = "isActive", source = "active")
    void updatePromotion(PromotionRequest request, @MappingTarget Promotion promotion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "title", source = "bannerTitle")
    @Mapping(target = "imageUrl", source = "bannerImage")
    @Mapping(target = "linkUrl", source = "bannerLink")
    @Mapping(target = "displayOrder", source = "bannerOrder")
    @Mapping(target = "isActive", source = "bannerActive")
    void updateBanner(PromotionRequest request, @MappingTarget Banner banner);

    default PromotionResponse toResponse(Promotion promotion, Banner banner, int used, int reserved) {
        return new PromotionResponse(
                promotion.getId(), promotion.getCode(), promotion.getName(),
                promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getUsageLimit(),
                promotion.getStartDate(), promotion.getEndDate(), Boolean.TRUE.equals(promotion.getIsActive()),
                used, reserved,
                banner == null ? "" : banner.getTitle(),
                banner == null ? "" : banner.getImageUrl(),
                banner == null ? "/san-pham" : banner.getLinkUrl(),
                banner == null || banner.getDisplayOrder() == null ? 0 : banner.getDisplayOrder(),
                banner != null && Boolean.TRUE.equals(banner.getIsActive())
        );
    }
}
