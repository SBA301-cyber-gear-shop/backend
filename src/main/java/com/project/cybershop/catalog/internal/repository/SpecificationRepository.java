package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, Integer> {
    Optional<Specification> findByName(String name);
    boolean existsByName(String name);
}
