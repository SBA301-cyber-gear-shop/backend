package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Integer> {

    @Query(value = """
            SELECT pu.id AS "id", pu.user_id AS "userId", u.full_name AS "customerName",
                   u.email AS "customerEmail", pu.order_id AS "orderId", o.order_code AS "orderCode",
                   o.created_at AS "orderCreatedAt", o.status AS "orderStatus",
                   pu.discount_amount AS "discountAmount", pu.status AS "status"
            FROM promotion_usages pu
            LEFT JOIN users u ON u.id=pu.user_id
            LEFT JOIN orders o ON o.id=pu.order_id
            WHERE pu.promotion_id=:promotionId
              AND (:status='ALL' OR CASE
                   WHEN pu.status IN ('RESERVED','APPLIED') THEN 'RESERVED'
                   WHEN pu.status IN ('REVOKED','CANCELED') THEN 'REVOKED'
                   ELSE pu.status END = :status)
              AND (lower(coalesce(u.full_name,'')) LIKE :search ESCAPE '!'
                   OR lower(coalesce(u.email,'')) LIKE :search ESCAPE '!'
                   OR lower(coalesce(o.order_code,'')) LIKE :search ESCAPE '!')
            ORDER BY o.created_at DESC NULLS LAST, pu.id DESC
            """, countQuery = """
            SELECT count(*) FROM promotion_usages pu
            LEFT JOIN users u ON u.id=pu.user_id
            LEFT JOIN orders o ON o.id=pu.order_id
            WHERE pu.promotion_id=:promotionId
              AND (:status='ALL' OR CASE
                   WHEN pu.status IN ('RESERVED','APPLIED') THEN 'RESERVED'
                   WHEN pu.status IN ('REVOKED','CANCELED') THEN 'REVOKED'
                   ELSE pu.status END = :status)
              AND (lower(coalesce(u.full_name,'')) LIKE :search ESCAPE '!'
                   OR lower(coalesce(u.email,'')) LIKE :search ESCAPE '!'
                   OR lower(coalesce(o.order_code,'')) LIKE :search ESCAPE '!')
            """, nativeQuery = true)
    Page<PromotionUsageHistoryProjection> findHistory(@Param("promotionId") Integer promotionId,
            @Param("status") String status, @Param("search") String search, Pageable pageable);

    boolean existsByPromotionIdAndUserId(Integer promotionId, Integer userId);

    boolean existsByPromotionId(Integer promotionId);

    int countByPromotionIdAndStatus(Integer promotionId, String status);

    @Query("""
            select count(u) from PromotionUsage u
            where u.promotion.id = :promotionId
              and (u.status is null or u.status not in ('REVOKED', 'CANCELED'))
            """)
    int countAllocated(@Param("promotionId") Integer promotionId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PromotionUsage u set u.status = 'USED'
            where u.orderId = :orderId and u.status = 'RESERVED'
            """)
    void markUsed(@Param("orderId") Integer orderId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PromotionUsage u set u.status = 'REVOKED'
            where u.orderId = :orderId and u.status in ('RESERVED', 'USED')
            """)
    void release(@Param("orderId") Integer orderId);
}
