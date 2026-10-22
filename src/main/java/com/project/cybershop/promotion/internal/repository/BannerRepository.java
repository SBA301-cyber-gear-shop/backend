package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {

    Optional<Banner> findFirstByPromotionIdOrderByIdAsc(Integer promotionId);
}
