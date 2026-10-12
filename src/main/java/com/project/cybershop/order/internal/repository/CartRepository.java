
package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    Optional<Cart> findByUserId(Integer userId);
}
