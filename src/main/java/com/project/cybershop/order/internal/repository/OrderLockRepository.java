package com.project.cybershop.order.internal.repository;

import com.project.cybershop.order.internal.entity.Order;

import java.util.Optional;

public interface OrderLockRepository {

    Optional<Order> findForUpdate(String orderCode, Integer userId);
}
