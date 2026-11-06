package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildTemplateSlotRepository extends JpaRepository<BuildTemplateSlot, Integer> {

    List<BuildTemplateSlot> findAllByOrderByStepOrderAsc();
}
