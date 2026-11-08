package com.project.cybershop.shipping.internal.repository;

import com.project.cybershop.shipping.internal.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {
    Optional<Shipment> findByOrderId(Integer orderId);
    Optional<Shipment> findByTrackingCode(String trackingCode);
    boolean existsByOrderId(Integer orderId);
}
