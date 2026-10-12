package com.project.cybershop.aftersales.internal.repository;

import com.project.cybershop.aftersales.internal.entity.ServiceRequest;
import com.project.cybershop.aftersales.internal.entity.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {
    List<ServiceRequest> findAllByOrderByCreatedAtDescIdDesc();

    List<ServiceRequest> findByStatusOrderByCreatedAtDescIdDesc(ServiceRequestStatus status);

    List<ServiceRequest> findByWarrantyCardIdInOrderByCreatedAtDescIdDesc(Collection<Integer> cardIds);

    boolean existsByWarrantyCardIdAndStatusNotIn(Integer cardId, Collection<ServiceRequestStatus> statuses);
}
