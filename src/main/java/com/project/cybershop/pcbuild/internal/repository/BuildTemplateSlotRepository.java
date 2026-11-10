package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildTemplateSlotRepository extends JpaRepository<BuildTemplateSlot, Integer> {

    List<BuildTemplateSlot> findAllByOrderByStepOrderAsc();

    boolean existsByCategoryId(Integer categoryId);

    boolean existsByStepOrder(Integer stepOrder);

    Optional<BuildTemplateSlot> findByCategoryId(Integer categoryId);

    Optional<BuildTemplateSlot> findByStepOrder(Integer stepOrder);

    boolean existsByRequiredIfCategoryId(Integer categoryId);
}
