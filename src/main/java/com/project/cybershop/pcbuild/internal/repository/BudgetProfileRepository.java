package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetProfileRepository extends JpaRepository<BudgetProfile, Integer> {

    List<BudgetProfile> findAllByOrderByIdAsc();
}
