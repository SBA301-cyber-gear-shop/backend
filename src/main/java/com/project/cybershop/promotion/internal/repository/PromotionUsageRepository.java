package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Integer> {

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
