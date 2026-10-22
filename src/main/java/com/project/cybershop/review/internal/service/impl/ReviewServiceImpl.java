package com.project.cybershop.review.internal.service.impl;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ProductReviewResponse;
import com.project.cybershop.review.api.dto.response.ProductFeedbackSummary;
import com.project.cybershop.review.internal.entity.Review;
import com.project.cybershop.review.internal.mapper.ReviewMapper;
import com.project.cybershop.review.internal.repository.ReviewRepository;
import com.project.cybershop.review.internal.service.ReviewService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final IdentityApi identityApi;
    private final Clock clock;

    @Override
    public void save(String email, Integer itemId, ReviewRequest request) {
        Integer userId = identityApi.requireActiveUser(email).getId();
        List<String> statuses = reviewRepository.findOwnedOrderStatusForUpdate(userId, itemId);
        if (statuses.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy dòng hàng đã mua.");
        }
        if (!"completed".equals(statuses.get(0))) {
            throw new BusinessException("Chỉ được đánh giá sau khi đã nhận hàng.");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5
                || request.comment() == null || request.comment().isBlank()) {
            throw new BusinessException("Đánh giá cần từ 1 đến 5 sao và có nhận xét.");
        }

        Review review = reviewRepository.findFirstByOrderItemIdOrderByIdAsc(itemId)
                .orElseGet(() -> {
                    Review created = new Review();
                    created.setOrderItemId(itemId);
                    return created;
                });

        reviewMapper.updateEntity(request, review);
        review.setReviewDate(LocalDateTime.now(clock));
        reviewRepository.saveAndFlush(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> forVariant(Integer variantId) {
        return reviewRepository.findPublishedByVariantId(variantId).stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, ProductFeedbackSummary> productSummaries() {
        Map<Integer, ProductFeedbackSummary> summaries = new LinkedHashMap<>();
        for (var row : reviewRepository.findProductSummaries()) {
            long count = row.getReviewCount();
            BigDecimal average = count == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(row.getRatingTotal())
                        .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
            summaries.put(row.getProductVariantId(), new ProductFeedbackSummary(
                    row.getProductVariantId(), average, count, row.getBuyerCount()
            ));
        }
        return summaries;
    }
}
