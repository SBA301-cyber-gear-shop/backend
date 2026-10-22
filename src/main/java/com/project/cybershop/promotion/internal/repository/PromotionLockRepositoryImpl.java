package com.project.cybershop.promotion.internal.repository;

import com.project.cybershop.promotion.internal.entity.Promotion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/** JPA repository fragment for checkout/admin writes that must share the same row lock. */
public class PromotionLockRepositoryImpl implements PromotionLockRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Promotion> findMatchingCodeForUpdate(String code) {
        List<Promotion> promotions = entityManager.createQuery("""
                        select p from Promotion p
                        where lower(p.code) = lower(:code)
                        order by p.id
                        """, Promotion.class)
                .setParameter("code", code)
                .setMaxResults(2)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        // Reload after acquiring the lock, including entities already read in this transaction.
        promotions.forEach(promotion -> entityManager.refresh(promotion, LockModeType.PESSIMISTIC_WRITE));
        return promotions;
    }

    @Override
    public Optional<Promotion> findByIdForUpdate(Integer id) {
        Promotion promotion = entityManager.find(Promotion.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (promotion != null) {
            entityManager.refresh(promotion, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(promotion);
    }
}
