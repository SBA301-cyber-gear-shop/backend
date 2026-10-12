package com.project.cybershop.aftersales.internal.repository;

import com.project.cybershop.aftersales.internal.entity.ServiceRequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceRequestEventRepository extends JpaRepository<ServiceRequestEvent, Integer> {
    List<ServiceRequestEvent> findByServiceRequestIdOrderByCreatedAtAscIdAsc(Integer serviceRequestId);

    List<ServiceRequestEvent> findByServiceRequestIdInOrderByCreatedAtAscIdAsc(Collection<Integer> serviceRequestIds);
}
