package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

public class OrderLockRepositoryImpl implements OrderLockRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Order> findForUpdate(String orderCode, Integer userId) {
        List<Order> orders = entityManager.createQuery("""
                        SELECT o FROM Order o
                        WHERE o.orderCode = :orderCode
                          AND (:userId IS NULL OR o.userId = :userId)
                        """, Order.class)
                .setParameter("orderCode", orderCode)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        // Đọc lại sau khi lấy khóa, kể cả khi entity đã có trong persistence context.
        orders.forEach(order -> entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE));
        return orders.stream().findFirst();
    }
}
