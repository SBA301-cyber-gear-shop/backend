package com.project.cybershop.review.internal.repository;

import com.project.cybershop.review.internal.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Optional<Review> findFirstByOrderItemIdOrderByIdAsc(Integer orderItemId);

    // The order belongs to another module. Lock its existing row without duplicating its entity.
    @Query(value = """
            SELECT o.status FROM orders o
            WHERE o.user_id = :userId
              AND o.id = (SELECT oi.order_id FROM order_items oi WHERE oi.id = :itemId)
            FOR UPDATE
            """, nativeQuery = true)
    List<String> findOwnedOrderStatusForUpdate(
            @Param("userId") Integer userId,
            @Param("itemId") Integer itemId
    );

    @Query(value = """
            SELECT CAST(r.id AS VARCHAR) AS id,
                   CAST(oi.product_variant_id AS VARCHAR) AS "productId",
                   r.rating AS rating, r.comment AS comment, u.full_name AS name
            FROM reviews r
            JOIN order_items oi ON oi.id = r.order_item_id
            JOIN orders o ON o.id = oi.order_id
            JOIN users u ON u.id = o.user_id
            WHERE oi.product_variant_id = :variantId AND o.status = 'completed'
            ORDER BY r.review_date DESC, r.id DESC
            """, nativeQuery = true)
    List<ProductReviewProjection> findPublishedByVariantId(@Param("variantId") Integer variantId);

    // Aggregate purchases separately: joining review rows must not multiply buyer counts.
    @Query(value = """
            SELECT pv.id AS "productVariantId",
                   COALESCE(ratings.rating_total, 0) AS "ratingTotal",
                   COALESCE(ratings.review_count, 0) AS "reviewCount",
                   COALESCE(purchases.buyer_count, 0) AS "buyerCount"
            FROM product_variants pv
            JOIN products p ON p.id = pv.product_id AND lower(p.status) = 'active'
            LEFT JOIN (
                SELECT oi.product_variant_id, SUM(r.rating) AS rating_total, COUNT(*) AS review_count
                FROM reviews r
                JOIN order_items oi ON oi.id = r.order_item_id
                JOIN orders o ON o.id = oi.order_id AND o.status = 'completed'
                GROUP BY oi.product_variant_id
            ) ratings ON ratings.product_variant_id = pv.id
            LEFT JOIN (
                SELECT oi.product_variant_id, COUNT(DISTINCT o.user_id) AS buyer_count
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id AND o.status = 'completed'
                GROUP BY oi.product_variant_id
            ) purchases ON purchases.product_variant_id = pv.id
            """, nativeQuery = true)
    List<ProductFeedbackProjection> findProductSummaries();
}
