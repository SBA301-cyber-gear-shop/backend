package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BudgetProfileRepository extends JpaRepository<BudgetProfile, Integer> {

    List<BudgetProfile> findAllByOrderByIdAsc();

    List<BudgetProfile> findByPurposeOrderByIdAsc(String purpose);

    List<BudgetProfile> findBySlotId(Integer slotId);

    boolean existsByPurpose(String purpose);

    @Modifying
    @Query("DELETE FROM BudgetProfile b WHERE b.purpose = :purpose")
    void deleteByPurpose(@Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM BudgetProfile b WHERE b.slot.id = :slotId")
    void deleteBySlotId(@Param("slotId") Integer slotId);
}
