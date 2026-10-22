
package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.order.internal.service.CartService;

import com.project.cybershop.order.api.dto.request.AddToCartRequest;
import com.project.cybershop.order.api.dto.request.UpdateCartItemRequest;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.entity.CartItem;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.order.internal.repository.CartItemRepository;

import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;


import com.project.cybershop.order.api.dto.response.CartResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductLookup productLookup;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    // =========================================
    // THÊM SẢN PHẨM VÀO GIỎ HÀNG
    // =========================================
    @Override
    @Transactional
    public void addToCart(
            Integer userId,
            AddToCartRequest request
    ) {

        lockUser(userId);

        // 1. Kiểm tra người dùng
        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    "Người dùng không hợp lệ"
            );
        }

        // 2. Kiểm tra request
        if (request == null
                || request.productVariantId() == null
                || request.productVariantId() <= 0
                || request.quantity() == null
                || request.quantity() <= 0) {

            throw new BusinessException(
                    "Thông tin sản phẩm hoặc số lượng không hợp lệ"
            );
        }

        // 3. Tìm sản phẩm từ database
        ProductLookup.ProductInfo product =
                productLookup.findByVariantId(
                        request.productVariantId()
                );

        if (product == null) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy sản phẩm hoặc sản phẩm đã ngừng kinh doanh"
            );
        }

        // 4. Kiểm tra tồn kho
        if (product.stockQuantity() == null
                || product.stockQuantity() <= 0) {

            throw new BusinessException(
                    "Sản phẩm đã hết hàng"
            );
        }

        // 5. Tìm hoặc tạo giỏ hàng
        Cart cart = cartRepository
                .findByUserId(userId)
                .orElseGet(() -> {

                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setUpdatedAt(LocalDateTime.now());

                    return cartRepository.save(newCart);
                });

        // 6. Kiểm tra sản phẩm đã tồn tại trong giỏ chưa
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductVariantId(
                        cart.getId(),
                        request.productVariantId()
                )
                .orElse(null);

        // 7. Tính tổng số lượng
        long newQuantity = request.quantity();

        if (cartItem != null) {
            newQuantity += cartItem.getQuantity();
        }

        // 8. Kiểm tra số lượng với tồn kho hiện tại
        if (newQuantity > product.stockQuantity()) {

            throw new BusinessException(
                    "Số lượng vượt quá tồn kho. Hiện còn "
                            + product.stockQuantity()
                            + " sản phẩm"
            );
        }

        // 9. Thêm mới hoặc cập nhật CartItem
        if (cartItem == null) {

            cartItem = new CartItem();

            cartItem.setCartId(cart.getId());

            cartItem.setProductVariantId(
                    request.productVariantId()
            );

            cartItem.setQuantity((int) newQuantity);

            cartItem.setCreatedAt(LocalDateTime.now());

        } else {
            cartItem.setQuantity((int) newQuantity);
        }

        cartItemRepository.save(cartItem);

        // 10. Cập nhật thời gian giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }


    // =========================================
    // XEM GIỎ HÀNG CỦA NGƯỜI DÙNG
    // =========================================
    @Override
    @Transactional(readOnly = true)
    public CartResponse getMyCart(Integer userId) {

        // 1. Kiểm tra người dùng
        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    "Người dùng không hợp lệ"
            );
        }

        // 2. Tìm giỏ hàng
        Cart cart = cartRepository
                .findByUserId(userId)
                .orElse(null);

        // 3. Chưa có giỏ hàng thì trả về giỏ rỗng
        if (cart == null) {
            return new CartResponse(
                    null,
                    userId,
                    List.of(),
                    0,
                    BigDecimal.ZERO
            );
        }

        // 4. Lấy danh sách sản phẩm trong giỏ
        List<CartItem> cartItems =
                cartItemRepository.findByCartId(cart.getId());

        List<CartResponse.CartItemResponse> items =
                new ArrayList<>();

        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 5. Duyệt từng sản phẩm
        for (CartItem cartItem : cartItems) {

            ProductLookup.ProductInfo product =
                    productLookup.findByVariantId(
                            cartItem.getProductVariantId()
                    );

            // Sản phẩm đã bị xóa hoặc ngừng kinh doanh
            if (product == null) {
                // Vẫn trả dòng giỏ để khách có thể xóa sản phẩm đã ngừng bán.
                items.add(new CartResponse.CartItemResponse(cartItem.getId(),
                        cartItem.getProductVariantId(), "Sản phẩm không còn kinh doanh",
                        "", null, BigDecimal.ZERO, cartItem.getQuantity(), BigDecimal.ZERO, 0, false));
                totalQuantity = Math.addExact(totalQuantity, cartItem.getQuantity());
                continue;
            }

            // 6. Kiểm tra dữ liệu số lượng
            Integer quantity = cartItem.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw new BusinessException(
                        "Số lượng trong giỏ hàng không hợp lệ"
                );
            }

            // 7. Tính tiền từng sản phẩm
            BigDecimal subtotal = product.price()
                    .multiply(BigDecimal.valueOf(quantity));

            // 8. Tạo DTO cho từng sản phẩm
            CartResponse.CartItemResponse itemResponse =
                    new CartResponse.CartItemResponse(
                            cartItem.getId(),
                            cartItem.getProductVariantId(),
                            product.productName(),
                            product.variantName(),
                            product.imageUrl(),
                            product.price(),
                            quantity,
                            subtotal,
                            product.stockQuantity(),
                            true
                    );

            items.add(itemResponse);

            // 9. Cộng số lượng và tổng tiền
            totalQuantity = Math.addExact(
                    totalQuantity,
                    quantity
            );

            totalAmount = totalAmount.add(subtotal);
        }

        // 10. Trả về thông tin giỏ hàng
        return new CartResponse(
                cart.getId(),
                userId,
                items,
                totalQuantity,
                totalAmount
        );
    }


    // =========================================
    // CẬP NHẬT SỐ LƯỢNG SẢN PHẨM TRONG GIỎ
    // =========================================
    @Override
    @Transactional
    public void updateCartItem(
            Integer userId,
            Integer cartItemId,
            UpdateCartItemRequest request
    ) {

        lockUser(userId);

        // 1. Kiểm tra user
        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    "Người dùng không hợp lệ"
            );
        }

        // 2. Kiểm tra ID dòng giỏ hàng
        if (cartItemId == null || cartItemId <= 0) {
            throw new BusinessException(
                    "ID sản phẩm trong giỏ không hợp lệ"
            );
        }

        // 3. Kiểm tra request
        if (request == null
                || request.quantity() == null
                || request.quantity() <= 0) {

            throw new BusinessException(
                    "Số lượng phải lớn hơn 0"
            );
        }

        // 4. Tìm giỏ hàng của user
        Cart cart = cartRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy giỏ hàng"
                        )
                );

        // 5. Tìm dòng sản phẩm cần cập nhật
        CartItem cartItem = cartItemRepository
                .findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy sản phẩm trong giỏ hàng"
                        )
                );

        // 6. Kiểm tra quyền sở hữu giỏ hàng
        if (!cart.getId().equals(cartItem.getCartId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy sản phẩm trong giỏ hàng"
            );
        }

        // 7. Kiểm tra sản phẩm còn kinh doanh
        ProductLookup.ProductInfo product =
                productLookup.findByVariantId(
                        cartItem.getProductVariantId()
                );

        if (product == null) {
            throw new ResourceNotFoundException(
                    "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh"
            );
        }

        // 8. Kiểm tra tồn kho
        if (product.stockQuantity() == null
                || request.quantity() > product.stockQuantity()) {

            throw new BusinessException(
                    "Số lượng vượt quá tồn kho. Hiện còn "
                            + product.stockQuantity()
                            + " sản phẩm"
            );
        }

        // 9. Cập nhật số lượng mới
        cartItem.setQuantity(request.quantity());

        cartItemRepository.save(cartItem);

        // 10. Cập nhật thời gian thay đổi giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }


    // =========================================
    // XÓA SẢN PHẨM KHỎI GIỎ HÀNG
    // =========================================
    @Override
    @Transactional
    public void removeCartItem(
            Integer userId,
            Integer cartItemId
    ) {

        lockUser(userId);

        // 1. Kiểm tra người dùng
        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    "Người dùng không hợp lệ"
            );
        }

        // 2. Kiểm tra ID dòng sản phẩm
        if (cartItemId == null || cartItemId <= 0) {
            throw new BusinessException(
                    "ID sản phẩm trong giỏ không hợp lệ"
            );
        }

        // 3. Tìm giỏ hàng của người dùng
        Cart cart = cartRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy giỏ hàng"
                        )
                );

        // 4. Tìm dòng sản phẩm cần xóa
        CartItem cartItem = cartItemRepository
                .findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy sản phẩm trong giỏ hàng"
                        )
                );

        // 5. Kiểm tra sản phẩm thuộc giỏ hàng của user
        if (!cart.getId().equals(cartItem.getCartId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy sản phẩm trong giỏ hàng"
            );
        }

        // 6. Xóa sản phẩm khỏi giỏ
        cartItemRepository.delete(cartItem);

        // 7. Cập nhật thời gian giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }

    private void lockUser(Integer userId) {
        if (userId == null || userId <= 0 || jdbc.queryForList(
                "SELECT id FROM users WHERE id=? AND is_active=true FOR UPDATE", Integer.class, userId).isEmpty()) {
            throw new BusinessException("Người dùng không hợp lệ hoặc đã bị khóa.");
        }
    }

}
