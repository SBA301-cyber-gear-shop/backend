
package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    // Lấy danh sách sản phẩm trong giỏ hàng
    List<CartItem> findByCartId(Integer cartId);

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
}
