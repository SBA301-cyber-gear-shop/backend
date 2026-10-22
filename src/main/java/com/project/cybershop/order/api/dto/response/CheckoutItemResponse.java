
package com.project.cybershop.order.api.dto.response;

import java.math.BigDecimal;

public record CheckoutItemResponse(

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
