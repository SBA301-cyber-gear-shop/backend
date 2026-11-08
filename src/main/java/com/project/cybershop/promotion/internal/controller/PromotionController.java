package com.project.cybershop.promotion.internal.controller;

import com.project.cybershop.promotion.api.dto.request.PromotionRequest;
import com.project.cybershop.promotion.api.dto.response.PromotionResponse;
import com.project.cybershop.promotion.api.dto.response.PromotionUsageHistoryResponse;
import com.project.cybershop.promotion.internal.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/api/promotions")
    public List<PromotionResponse> visible() {
        return promotionService.publicList();
    }

    @GetMapping("/api/admin/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PromotionResponse> list(Authentication authentication) {
        return promotionService.list(authentication.getName());
    }

    @PostMapping("/api/admin/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    public PromotionResponse create(
            Authentication authentication,
            @Valid @RequestBody PromotionRequest request
    ) {
        return promotionService.save(authentication.getName(), null, request);
    }

    @GetMapping("/api/admin/promotions/{id}/usages")
    @PreAuthorize("hasRole('ADMIN')")
    public PromotionUsageHistoryResponse history(Authentication authentication, @PathVariable Integer id,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="ALL") String status, @RequestParam(defaultValue="") String search) {
        return promotionService.history(authentication.getName(), id, page, size, status, search);
    }

    @PutMapping("/api/admin/promotions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PromotionResponse update(
            Authentication authentication,
            @PathVariable Integer id,
            @Valid @RequestBody PromotionRequest request
    ) {
        return promotionService.save(authentication.getName(), id, request);
    }
}
