package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer>, PromotionLockRepository {

    List<Promotion> findAllByOrderByIdDesc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    @Query("""
            select new com.project.cybershop.promotion.internal.repository.PromotionTerms(
                p.id, p.code, p.discountType, p.discountValue, p.usageLimit,
                p.isActive, p.startDate, p.endDate, p.audience
            )
            from Promotion p
            where lower(p.code) = lower(:code)
            order by p.id
            """)
    List<PromotionTerms> findTermsByCodeIgnoreCase(@Param("code") String code);
}
