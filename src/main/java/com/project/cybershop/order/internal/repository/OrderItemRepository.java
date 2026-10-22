package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query(value = """
            SELECT oi.id AS "id", oi.product_variant_id AS "productVariantId",
                   oi.product_name AS "productName", oi.quantity AS "quantity",
                   oi.unit_price AS "unitPrice", oi.subtotal AS "subtotal",
                   oi.warranty_period_months AS "warrantyPeriodMonths",
                   r.rating AS "rating", r.comment AS "reviewComment"
            FROM order_items oi
            LEFT JOIN reviews r ON r.order_item_id = oi.id
            WHERE oi.order_id = :orderId
            ORDER BY oi.id
            """, nativeQuery = true)
    List<OrderItemProjection> findResponseItems(@Param("orderId") Integer orderId);
}
