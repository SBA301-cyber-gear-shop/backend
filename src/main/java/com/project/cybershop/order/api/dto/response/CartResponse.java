
package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        Integer cartId,

        Integer userId,

        List<CartItemResponse> items,

        Integer totalQuantity,

        BigDecimal totalAmount

) {

    // Thông tin từng sản phẩm trong giỏ hàng
    public record CartItemResponse(

            Integer cartItemId,

            Integer productVariantId,

            String productName,

            String variantName,

            String imageUrl,

            BigDecimal unitPrice,

            Integer quantity,

            BigDecimal subtotal,

            Integer stockQuantity,

            boolean available,

            Integer pcCustomId,

            String pcCustomName

    ) {
        public CartItemResponse(
                Integer cartItemId,
                Integer productVariantId,
                String productName,
                String variantName,
                String imageUrl,
                BigDecimal unitPrice,
                Integer quantity,
                BigDecimal subtotal,
                Integer stockQuantity,
                boolean available
        ) {
            this(cartItemId, productVariantId, productName, variantName, imageUrl, unitPrice, quantity, subtotal, stockQuantity, available, null, null);
        }
    }
}
