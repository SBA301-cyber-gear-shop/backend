package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.order.api.dto.response.CheckoutItemResponse;
import com.project.cybershop.order.api.dto.response.CheckoutPreviewResponse;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.mapper.CheckoutMapper;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.order.internal.repository.CheckoutItemProjection;
import com.project.cybershop.order.internal.repository.CheckoutRepository;
import com.project.cybershop.order.internal.service.CheckoutService;
import com.project.cybershop.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutServiceImpl implements CheckoutService {

    private final CheckoutRepository checkoutRepository;
    private final CartRepository cartRepository;
    private final CheckoutMapper checkoutMapper;
    private final com.project.cybershop.promotion.api.PromotionApi promotions;

    @Override
    @Transactional
    public CheckoutPreviewResponse validate(String loginEmail) {
        return validate(loginEmail, null);
    }

    // Kiểm tra giỏ và tính tổng tiền; chưa tạo đơn hoặc thay đổi tồn kho.
    @Override
    @Transactional
    public CheckoutPreviewResponse validate(String loginEmail, List<Integer> itemVariantIds) {
        return validate(loginEmail, null, itemVariantIds);
    }

    @Override
    @Transactional
    public CheckoutPreviewResponse validate(String loginEmail, List<Integer> cartItemIds, List<Integer> itemVariantIds) {
        Integer userId = checkoutRepository.findActiveUserId(loginEmail)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang hoạt động."));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có giỏ hàng."));

        List<CheckoutItemProjection> cartItems = checkoutRepository.findItemsForPreview(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống.");
        }

        if (cartItemIds != null && !cartItemIds.isEmpty()) {
            if (cartItemIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new BusinessException("Danh sách sản phẩm được chọn không hợp lệ.");
            }
            Set<Integer> allowedCartIds = new HashSet<>(cartItemIds);
            cartItems = cartItems.stream()
                    .filter(item -> allowedCartIds.contains(item.getCartItemId()))
                    .toList();
            Set<Integer> foundCartItemIds = cartItems.stream()
                    .map(CheckoutItemProjection::getCartItemId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!foundCartItemIds.equals(allowedCartIds)) {
                throw new BusinessException("Sản phẩm được chọn đã thay đổi. Vui lòng kiểm tra lại giỏ hàng.");
            }
        } else if (itemVariantIds != null) {
            if (itemVariantIds.isEmpty()
                    || itemVariantIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new BusinessException("Danh sách sản phẩm được chọn không hợp lệ.");
            }
            Set<Integer> allowed = new HashSet<>(itemVariantIds);
            cartItems = cartItems.stream()
                    .filter(item -> allowed.contains(item.getProductVariantId()))
                    .toList();
            Set<Integer> foundVariantIds = cartItems.stream()
                    .map(CheckoutItemProjection::getProductVariantId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!foundVariantIds.equals(allowed)) {
                throw new BusinessException("Sản phẩm được chọn đã thay đổi. Vui lòng kiểm tra lại giỏ hàng.");
            }
        }

        var selectedVariantIds = cartItems.stream().map(CheckoutItemProjection::getProductVariantId).collect(java.util.stream.Collectors.toSet());
        promotions.syncProductPrices(selectedVariantIds.stream().toList());
        var selectedCartIds = cartItems.stream().map(CheckoutItemProjection::getCartItemId).collect(java.util.stream.Collectors.toSet());
        cartItems = checkoutRepository.findItemsForPreview(cart.getId()).stream()
                .filter(item -> selectedCartIds.contains(item.getCartItemId())).toList();

        // Kiểm tra tổng số lượng yêu cầu theo từng variant trên toàn bộ các dòng được chọn
        // (xử lý chính xác trường hợp 1 linh kiện trong set PC + 1 linh kiện mua lẻ cùng loại)
        java.util.Map<Integer, Integer> totalRequestedPerVariant = new java.util.HashMap<>();
        for (CheckoutItemProjection ci : cartItems) {
            totalRequestedPerVariant.merge(ci.getProductVariantId(), ci.getQuantity(), Integer::sum);
        }

        List<CheckoutItemResponse> items = cartItems.stream()
                .map(item -> validateAndMapItem(item, totalRequestedPerVariant.get(item.getProductVariantId())))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CheckoutItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutPreviewResponse(cart.getId(), items, totalAmount, true);
    }

    private CheckoutItemResponse validateAndMapItem(CheckoutItemProjection item, Integer totalRequestedQty) {
        if (item.getExistingVariantId() == null || item.getExistingProductId() == null) {
            throw new BusinessException(
                    "Sản phẩm trong giỏ không còn tồn tại: variant " + item.getProductVariantId()
            );
        }

        if (!"active".equalsIgnoreCase(item.getProductStatus())) {
            throw new BusinessException("Sản phẩm đã ngừng bán: " + item.getProductName());
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessException("Số lượng không hợp lệ: " + item.getProductName());
        }

        int requiredQty = totalRequestedQty != null ? totalRequestedQty : item.getQuantity();
        if (item.getStockQuantity() == null || requiredQty > item.getStockQuantity()) {
            throw new BusinessException("Không đủ tồn kho: " + item.getProductName()
                    + " (yêu cầu " + requiredQty + ", hiện còn " + (item.getStockQuantity() == null ? 0 : item.getStockQuantity()) + ")");
        }

        if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
            throw new BusinessException("Giá sản phẩm không hợp lệ: " + item.getProductName());
        }

        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return checkoutMapper.toItemResponse(item, subtotal);
    }
}
