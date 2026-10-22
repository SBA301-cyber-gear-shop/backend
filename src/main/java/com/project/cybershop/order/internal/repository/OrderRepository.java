package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, OrderLockRepository {

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserId(String orderCode, Integer userId);

    List<Order> findByUserIdOrderByIdDesc(Integer userId);

    @Query("""
            SELECT o FROM Order o
            WHERE (:status = '' OR o.status = :status)
              AND (lower(o.orderCode) LIKE :term
                   OR lower(coalesce(o.pickupName, '')) LIKE :term
                   OR coalesce(o.pickupPhone, '') LIKE :term)
            ORDER BY o.id DESC
            """)
    List<Order> findForOperations(
            @Param("status") String status,
            @Param("term") String term,
            Pageable pageable
    );

    @Query(value = """
            SELECT p.code
            FROM promotion_usages u
            JOIN promotions p ON p.id = u.promotion_id
            WHERE u.order_id = :orderId
            """, nativeQuery = true)
    Optional<String> findPromotionCode(@Param("orderId") Integer orderId);

    @Query(value = """
            SELECT id FROM payment_methods
            WHERE id = :methodId AND user_id = :userId
            """, nativeQuery = true)
    Optional<Integer> findOwnedPaymentMethodId(
            @Param("methodId") Integer methodId,
            @Param("userId") Integer userId
    );
}
