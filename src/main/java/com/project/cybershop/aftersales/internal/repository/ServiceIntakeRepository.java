package com.project.cybershop.aftersales.internal.repository;

import com.project.cybershop.aftersales.internal.entity.ServiceIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceIntakeRepository extends JpaRepository<ServiceIntake, Integer> {

    List<ServiceIntake> findByUserIdOrderByCreatedAtDescIdDesc(Integer userId);
}
