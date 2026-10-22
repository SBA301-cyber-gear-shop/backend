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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutServiceImpl implements CheckoutService {

    private final CheckoutRepository checkoutRepository;
    private final CartRepository cartRepository;
    private final CheckoutMapper checkoutMapper;

    // Kiểm tra giỏ và tính tổng tiền; chưa tạo đơn hoặc thay đổi tồn kho.
    @Override
    public CheckoutPreviewResponse validate(String loginEmail) {
        Integer userId = checkoutRepository.findActiveUserId(loginEmail)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang hoạt động."));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Bạn chưa có giỏ hàng."));

        List<CheckoutItemProjection> cartItems = checkoutRepository.findItemsForPreview(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống.");
        }

        List<CheckoutItemResponse> items = cartItems.stream()
                .map(this::validateAndMapItem)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CheckoutItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutPreviewResponse(cart.getId(), items, totalAmount, true);
    }

    private CheckoutItemResponse validateAndMapItem(CheckoutItemProjection item) {
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

        if (item.getStockQuantity() == null || item.getQuantity() > item.getStockQuantity()) {
            throw new BusinessException("Không đủ tồn kho: " + item.getProductName());
        }

        if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
            throw new BusinessException("Giá sản phẩm không hợp lệ: " + item.getProductName());
        }

        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return checkoutMapper.toItemResponse(item, subtotal);
    }
}
