
package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.AddToCartRequest;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.entity.CartItem;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.order.internal.repository.CartItemRepository;
import com.project.cybershop.order.internal.repository.ProductLookup;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductLookup productLookup;

    // =========================================
    // THÊM SẢN PHẨM VÀO GIỎ HÀNG
    // =========================================
    @Transactional
    public void addToCart(
            Integer userId,
            AddToCartRequest request
    ) {

        // 1. Kiểm tra người dùng
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Người dùng không hợp lệ"
            );
        }

        if (request == null
                || request.productVariantId() == null
                || request.productVariantId() <= 0
                || request.quantity() == null
                || request.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Thông tin sản phẩm hoặc số lượng không hợp lệ"
            );
        }

        // 2. Kiểm tra sản phẩm từ database
        ProductLookup.ProductInfo product =
                productLookup.findByVariantId(
                        request.productVariantId()
                );

        if (product == null) {
            throw new IllegalArgumentException(
                    "Sản phẩm không tồn tại hoặc đã ngừng bán"
            );
        }

        // 3. Kiểm tra tồn kho
        if (product.stockQuantity() == null
                || product.stockQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Sản phẩm đã hết hàng"
            );
        }

        // 4. Tìm giỏ hàng của người dùng
        Cart cart = cartRepository
                .findByUserId(userId)
                .orElseGet(() -> {

                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setUpdatedAt(
                            LocalDateTime.now()
                    );

                    return cartRepository.save(newCart);
                });

        // 5. Tìm sản phẩm trong giỏ
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductVariantId(
                        cart.getId(),
                        request.productVariantId()
                )
                .orElse(null);

        // 6. Tính tổng số lượng muốn mua
        long newQuantity = request.quantity();

        if (cartItem != null) {
            newQuantity += cartItem.getQuantity();
        }

        // 7. Kiểm tra tồn kho lần nữa theo tổng số lượng
        if (newQuantity > product.stockQuantity()) {
            throw new IllegalArgumentException(
                    "Số lượng vượt quá tồn kho. Hiện còn "
                            + product.stockQuantity() + " sản phẩm"
            );
        }

        // 8. Thêm mới hoặc cập nhật CartItem
        if (cartItem == null) {

            cartItem = new CartItem();

            cartItem.setCartId(cart.getId());

            cartItem.setProductVariantId(
                    request.productVariantId()
            );

            cartItem.setQuantity((int) newQuantity);

            cartItem.setCreatedAt(
                    LocalDateTime.now()
            );

        } else {

            cartItem.setQuantity((int) newQuantity);

        }

        cartItemRepository.save(cartItem);

        // 9. Cập nhật thời gian giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(cart);
    }
}
