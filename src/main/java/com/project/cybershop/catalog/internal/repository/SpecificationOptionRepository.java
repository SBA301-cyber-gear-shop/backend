package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.SpecificationOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecificationOptionRepository extends JpaRepository<SpecificationOption, Integer> {
    List<SpecificationOption> findBySpecificationId(Integer specificationId);
    Optional<SpecificationOption> findBySpecificationIdAndCode(Integer specificationId, String code);
    boolean existsBySpecificationIdAndCode(Integer specificationId, String code);
}
