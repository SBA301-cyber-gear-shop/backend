package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.Promotion;

import java.util.List;
import java.util.Optional;

public interface PromotionLockRepository {

    List<Promotion> findMatchingCodeForUpdate(String code);

    Optional<Promotion> findByIdForUpdate(Integer id);
}
