
package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    // Tìm giỏ hàng của người dùng
    Optional<Cart> findByUserId(Integer userId);

    // Kiểm tra người dùng đã có giỏ hàng hay chưa
    boolean existsByUserId(Integer userId);
}
