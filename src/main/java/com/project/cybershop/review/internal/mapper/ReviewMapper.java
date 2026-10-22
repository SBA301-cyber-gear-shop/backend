package com.project.cybershop.review.internal.mapper;

import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ProductReviewResponse;
import com.project.cybershop.review.internal.entity.Review;
import com.project.cybershop.review.internal.repository.ProductReviewProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderItemId", ignore = true)
    @Mapping(target = "reviewDate", ignore = true)
    @Mapping(target = "comment", expression = "java(request.comment().trim())")
    void updateEntity(ReviewRequest request, @MappingTarget Review review);

    ProductReviewResponse toResponse(ProductReviewProjection review);
}
