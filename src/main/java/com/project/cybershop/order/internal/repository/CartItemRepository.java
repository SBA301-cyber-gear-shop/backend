package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    // Sắp theo id để giữ thứ tự thêm vào giỏ: không có ORDER BY thì Postgres có thể trả dòng vừa UPDATE xuống cuối.
    List<CartItem> findByCartIdOrderByIdAsc(Integer cartId);

    @Query(value = """
            SELECT product_variant_id AS "productVariantId", quantity AS "quantity"
            FROM cart_items
            WHERE cart_id = :cartId
            ORDER BY product_variant_id, id
            FOR UPDATE
            """, nativeQuery = true)
    List<CartLineProjection> findForCheckout(@Param("cartId") Integer cartId);

    // Tìm một phiên bản sản phẩm đã tồn tại trong giỏ
    Optional<CartItem> findByCartIdAndProductVariantId(
            Integer cartId,
            Integer productVariantId
    );

    // Kiểm tra phiên bản đã có trong giỏ hay chưa
    boolean existsByCartIdAndProductVariantId(
            Integer cartId,
            Integer productVariantId
    );

    // Xóa toàn bộ sản phẩm trong một giỏ hàng
    void deleteByCartId(Integer cartId);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId", nativeQuery = true)
    void clearAfterCheckout(@Param("cartId") Integer cartId);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId AND product_variant_id IN (:variantIds)", nativeQuery = true)
    void deleteByCartIdAndProductVariantIdIn(@Param("cartId") Integer cartId, @Param("variantIds") Collection<Integer> variantIds);
}
