
package com.project.cybershop.order.api.dto;

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

            Integer stockQuantity

    ) {
    }
}
