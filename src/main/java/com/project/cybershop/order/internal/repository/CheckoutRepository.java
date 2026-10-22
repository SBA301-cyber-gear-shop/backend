package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.Cart;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CheckoutRepository extends Repository<Cart, Integer> {

    @Query(value = """
            SELECT id FROM users
            WHERE lower(email) = lower(:email) AND is_active = true
            """, nativeQuery = true)
    Optional<Integer> findActiveUserId(@Param("email") String email);

    // Dùng LEFT JOIN để vẫn phát hiện dòng giỏ có phiên bản/sản phẩm đã bị xóa.
    @Query(value = """
            SELECT ci.product_variant_id AS "productVariantId",
                   ci.quantity AS "quantity",
                   pv.id AS "existingVariantId",
                   p.id AS "existingProductId",
                   p.name AS "productName",
                   p.status AS "productStatus",
                   pv.variant_name AS "variantName",
                   pv.image_url AS "imageUrl",
                   pv.price AS "unitPrice",
                   pv.stock_quantity AS "stockQuantity"
            FROM cart_items ci
            LEFT JOIN product_variants pv ON pv.id = ci.product_variant_id
            LEFT JOIN products p ON p.id = pv.product_id
            WHERE ci.cart_id = :cartId
            ORDER BY ci.id
            """, nativeQuery = true)
    List<CheckoutItemProjection> findItemsForPreview(@Param("cartId") Integer cartId);
}
