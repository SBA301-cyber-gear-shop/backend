package com.project.cybershop.promotion.internal.service.impl;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.promotion.api.dto.request.PromotionRequest;
import com.project.cybershop.promotion.api.dto.response.PromotionResponse;
import com.project.cybershop.promotion.api.dto.response.PromotionUsageHistoryResponse;
import org.springframework.data.domain.PageRequest;
import com.project.cybershop.promotion.internal.entity.Banner;
import com.project.cybershop.promotion.internal.entity.Promotion;
import com.project.cybershop.promotion.internal.entity.PromotionUsage;
import com.project.cybershop.promotion.internal.mapper.PromotionMapper;
import com.project.cybershop.promotion.internal.repository.BannerRepository;
import com.project.cybershop.promotion.internal.repository.PromotionRepository;
import com.project.cybershop.promotion.internal.repository.PromotionTerms;
import com.project.cybershop.promotion.internal.repository.PromotionUsageRepository;
import com.project.cybershop.promotion.internal.service.PromotionService;
import com.project.cybershop.promotion.internal.service.ProductSalePriceService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository usageRepository;
    private final BannerRepository bannerRepository;
    private final PromotionMapper promotionMapper;
    private final IdentityApi identityApi;
    private final Clock clock;
    private final ProductSalePriceService productSalePrices;

    @Override
    @Transactional(readOnly = true)
    public PromotionUsageHistoryResponse history(String email, Integer id, int page, int size, String status, String search) {
        requireAdmin(email);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new BusinessException("Trang không hợp lệ; số dòng mỗi trang từ 1 đến 100.");
        String selectedStatus = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "RESERVED", "USED", "REVOKED").contains(selectedStatus))
            throw new BusinessException("Trạng thái lượt dùng voucher không hợp lệ.");
        String term = search == null ? "" : search.trim();
        if (term.length() > 100) throw new BusinessException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình."));
        String pattern = "%" + term.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var result = usageRepository.findHistory(id, selectedStatus, pattern, PageRequest.of(page, size));
        var items = result.getContent().stream().map(row -> new PromotionUsageHistoryResponse.Item(
                row.getId(), row.getUserId(), row.getCustomerName(), row.getCustomerEmail(), row.getOrderId(),
                row.getOrderCode(), row.getOrderCreatedAt(), row.getOrderStatus(), row.getDiscountAmount(), row.getStatus())).toList();
        return new PromotionUsageHistoryResponse(id, promotion.getCode(), page, size, result.getTotalElements(), result.getTotalPages(), items);
    }

    @Override
    public void syncProductPrices(List<Integer> variantIds) { productSalePrices.sync(variantIds); }

    @Override
    public Quote quote(Integer userId, String code, BigDecimal total, boolean lock) {
        if (code == null || code.isBlank()) {
            return new Quote(null, null, BigDecimal.ZERO);
        }

        String normalizedCode = code.trim();
        List<PromotionTerms> matches = lock
                ? promotionRepository.findMatchingCodeForUpdate(normalizedCode).stream()
                    .map(PromotionTerms::from).toList()
                : promotionRepository.findTermsByCodeIgnoreCase(normalizedCode);

        if (matches.isEmpty()) {
            throw new BusinessException("Mã voucher không tồn tại.");
        }
        if (matches.size() != 1) {
            throw new BusinessException("Mã voucher bị trùng trong hệ thống.");
        }

        PromotionTerms promotion = matches.get(0);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!Boolean.TRUE.equals(promotion.active())
                || now.isBefore(promotion.startDate())
                || now.isAfter(promotion.endDate())) {
            throw new BusinessException("Voucher không hoạt động hoặc ngoài thời hạn sử dụng.");
        }

        // SQL hiện tại chỉ cho một lượt theo cặp user/promotion, kể cả đơn đã hủy.
        if (usageRepository.existsByPromotionIdAndUserId(promotion.id(), userId)) {
            throw new BusinessException("Bạn đã sử dụng voucher này.");
        }
        if (promotion.usageLimit() <= 0
                || usageRepository.countAllocated(promotion.id()) >= promotion.usageLimit()) {
            throw new BusinessException("Voucher đã hết lượt sử dụng.");
        }

        BigDecimal discount = calculateDiscount(promotion, total);
        return new Quote(promotion.id(), promotion.code(), discount.min(total));
    }

    @Override
    public void reserve(Quote quote, Integer userId, Integer orderId, boolean paid) {
        if (quote.promotionId() == null) {
            return;
        }

        PromotionUsage usage = new PromotionUsage();
        usage.setPromotion(promotionRepository.getReferenceById(quote.promotionId()));
        usage.setUserId(userId);
        usage.setOrderId(orderId);
        usage.setStatus(paid ? "USED" : "RESERVED");
        usage.setDiscountAmount(quote.discount());
        usageRepository.saveAndFlush(usage);
    }

    @Override
    public void markUsed(Integer orderId) {
        usageRepository.markUsed(orderId);
    }

    @Override
    public void release(Integer orderId) {
        usageRepository.release(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> list(String email) {
        requireAdmin(email);
        return readAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> publicList() {
        LocalDateTime now = LocalDateTime.now(clock);
        return readAll().stream()
                .filter(promotion -> promotion.active() && promotion.bannerActive())
                .filter(promotion -> !now.isBefore(promotion.start()) && !now.isAfter(promotion.end()))
                .filter(promotion -> promotion.used() + promotion.reserved() < promotion.limit())
                .toList();
    }

    @Override
    public PromotionResponse save(String email, Integer id, PromotionRequest request) {
        requireAdmin(email);
        validateRequest(request);

        Promotion promotion = id == null
                ? new Promotion()
                : promotionRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình."));

        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (id != null) {
            validateExistingPromotion(promotion, code, request.limit());
        }

        boolean duplicate = id == null
                ? promotionRepository.existsByCodeIgnoreCase(code)
                : promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (duplicate) {
            throw new BusinessException("Mã giảm giá đã tồn tại.");
        }

        promotionMapper.updatePromotion(request, promotion);
        Promotion saved = promotionRepository.saveAndFlush(promotion);
        saveBanner(saved, request);
        return toResponse(saved);
    }

    private BigDecimal calculateDiscount(PromotionTerms promotion, BigDecimal total) {
        BigDecimal value = promotion.discountValue();
        if (value == null || value.signum() < 0) {
            throw new BusinessException("Giá trị voucher không hợp lệ.");
        }

        String type = promotion.discountType() == null
                ? "" : promotion.discountType().trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "percent", "percentage" -> {
                if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessException("Phần trăm giảm giá không hợp lệ.");
                }
                yield total.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case "fixed", "fixed_amount" -> value;
            default -> throw new BusinessException("Loại voucher chưa được hỗ trợ.");
        };
    }

    private void validateRequest(PromotionRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{1,50}") || request.name().isBlank()) {
            throw new BusinessException("Mã voucher chỉ dùng chữ, số, dấu gạch ngang hoặc gạch dưới.");
        }
        if (!List.of("percentage", "fixed_amount").contains(request.type())) {
            throw new BusinessException("Loại giảm giá không hợp lệ.");
        }
        if (request.value().signum() <= 0
                || request.type().equals("percentage")
                    && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Giá trị giảm giá không hợp lệ.");
        }
        if (!request.end().isAfter(request.start())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        }

        String link = bannerLink(request);
        String image = bannerImage(request);
        if (!link.startsWith("/") || link.startsWith("//") || link.contains("\\")) {
            throw new BusinessException("Banner cần dùng đường dẫn nội bộ.");
        }
        boolean internalImage = image.startsWith("/") && !image.startsWith("//");
        if ((!image.isBlank() && !image.startsWith("https://") && !internalImage) || image.contains("\\")) {
            throw new BusinessException("Ảnh banner cần là đường dẫn nội bộ hoặc HTTPS.");
        }
        if (request.bannerActive() && image.isBlank()) {
            throw new BusinessException("Cần ảnh trước khi bật banner.");
        }
    }

    private void validateExistingPromotion(Promotion promotion, String code, int limit) {
        if (limit < usageRepository.countAllocated(promotion.getId())) {
            throw new BusinessException("Hạn mức không được thấp hơn lượt đã dùng và đang giữ.");
        }
        if (!promotion.getCode().equals(code) && usageRepository.existsByPromotionId(promotion.getId())) {
            throw new BusinessException("Mã đã gắn với đơn hàng không được đổi.");
        }
    }

    private void saveBanner(Promotion promotion, PromotionRequest request) {
        Banner banner = bannerRepository.findFirstByPromotionIdOrderByIdAsc(promotion.getId())
                .orElse(null);
        if (banner == null && bannerImage(request).isBlank()) {
            return;
        }
        if (banner == null) {
            banner = new Banner();
            banner.setPromotion(promotion);
        }

        promotionMapper.updateBanner(request, banner);
        banner.setImageUrl(bannerImage(request));
        banner.setLinkUrl(bannerLink(request));
        bannerRepository.saveAndFlush(banner);
    }

    private String bannerImage(PromotionRequest request) {
        return request.bannerImage() == null ? "" : request.bannerImage().trim();
    }

    private String bannerLink(PromotionRequest request) {
        return request.bannerLink() == null || request.bannerLink().isBlank()
                ? "/san-pham" : request.bannerLink().trim();
    }

    private void requireAdmin(String email) {
        if (!"Admin".equalsIgnoreCase(identityApi.requireActiveUser(email).getRole())) {
            throw new AccessDeniedException("Chỉ Admin được quản lý khuyến mãi.");
        }
    }

    private List<PromotionResponse> readAll() {
        return promotionRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private PromotionResponse toResponse(Promotion promotion) {
        Banner banner = bannerRepository.findFirstByPromotionIdOrderByIdAsc(promotion.getId())
                .orElse(null);
        int used = usageRepository.countByPromotionIdAndStatus(promotion.getId(), "USED");
        int reserved = usageRepository.countByPromotionIdAndStatus(promotion.getId(), "RESERVED");
        return promotionMapper.toResponse(promotion, banner, used, reserved);
    }
}
