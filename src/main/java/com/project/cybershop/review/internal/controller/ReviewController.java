package com.project.cybershop.review.internal.controller;

import com.project.cybershop.review.api.dto.request.ReviewRequest;
import com.project.cybershop.review.api.dto.response.ProductReviewResponse;
import com.project.cybershop.review.internal.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PutMapping("/api/order-items/{itemId}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(
            Authentication authentication,
            @PathVariable Integer itemId,
            @Valid @RequestBody ReviewRequest request
    ) {
        reviewService.save(authentication.getName(), itemId, request);
    }

    @GetMapping("/api/products/{variantId}/reviews")
    public List<ProductReviewResponse> list(@PathVariable Integer variantId) {
        return reviewService.forVariant(variantId);
    }
}
